from pathlib import Path
import json
import re
import sqlite3

root = Path(__file__).resolve().parents[1]
generated = root / 'app/build/generated/ksp/qaDebug/kotlin/com/project/solaria_mobile/core/database/AppDatabase_Impl.kt'
source = root / 'app/src/main/kotlin/com/project/solaria_mobile/core/database/ChatMigrations.kt'
schema = re.findall(r'connection.execSQL\("(CREATE (?:TABLE|UNIQUE INDEX|INDEX).*?)"\)', generated.read_text(encoding='utf-8'))
schema = [statement for statement in schema if 'room_master_table' not in statement]
migrations = re.findall(r'db.execSQL\("(.+?)"\)', source.read_text(encoding='utf-8'))
assert schema and migrations
fresh = sqlite3.connect(':memory:')
for statement in schema:
    fresh.execute(statement)

for start in (1, 2):
    existing = sqlite3.connect(':memory:')
    for statement in schema:
        if 'pending_messages' in statement:
            existing.execute(statement)
    existing.execute("INSERT INTO pending_messages VALUES(1, 'client-stable', 'actor', 'conversation', 'USER_TO_USER', 'Não perder esta mensagem', 1000, 2, 1)")
    if start == 2:
        for statement in migrations:
            if statement.startswith('CREATE'):
                existing.execute(statement)
        existing.execute("INSERT INTO conversations VALUES('c1', 'epoch', 7, 'DIRECT', 'actor,other', NULL, 0, 1000, 'm1', 1, 'texto', 0, 0)")
        existing.execute("INSERT INTO messages VALUES('m1', 'c1', 1, 'other', NULL, 'histórico', 0, 0, 1000, 7)")
    for statement in migrations:
        if start == 1 or not statement.startswith('CREATE'):
            existing.execute(statement)
    for (table,) in fresh.execute("SELECT name FROM sqlite_master WHERE type='table' AND name != 'sqlite_sequence'"):
        def columns(db):
            return [(name, kind, required, primary) for _, name, kind, required, _, primary in db.execute(f'PRAGMA table_info(`{table}`)')]
        assert columns(existing) == columns(fresh), table
        def indexes(db):
            return sorted((name, unique) for _, name, unique, *_ in db.execute(f'PRAGMA index_list(`{table}`)'))
        assert indexes(existing) == indexes(fresh), table
    assert existing.execute('SELECT clientMessageId, content, attempts, failed FROM pending_messages').fetchone() == ('client-stable', 'Não perder esta mensagem', 2, 1)
    existing.execute("INSERT OR IGNORE INTO pending_messages(clientMessageId, senderId, conversationId, messageType, content, createdAt, attempts, failed) VALUES('client-stable', 'actor', 'conversation', 'USER_TO_USER', 'Não perder esta mensagem', 1000, 2, 1)")
    assert existing.execute('SELECT COUNT(*) FROM pending_messages').fetchone() == (1,)
    if start == 2:
        assert existing.execute('SELECT content FROM messages').fetchone() == ('histórico',)
        assert existing.execute('SELECT lastReadSequence, localReadSequence, unreadCount, muted FROM conversations').fetchone() == (0, 0, 0, 0)
print(json.dumps({'migrations': ['1->3', '2->3'], 'schema_matches_room': True, 'pending_messages_preserved': True, 'history_preserved': True, 'duplicate_client_id_ignored': True}))
