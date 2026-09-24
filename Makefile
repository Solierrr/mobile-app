SHELL := /bin/sh

GRADLEW := ./gradlew
ORG_SCRIPTS_DIR ?= $(HOME)/.local/share/solierrr-infra-scripts
ORG_SCRIPTS_REPO ?= https://github.com/Solierrr/infra-scripts.git
ORG_SCRIPTS_POWERSHELL ?= powershell
EXTRACT_ENV := $(ORG_SCRIPTS_DIR)/scripts/extract-env.ps1
SERVICE := mobile-app
ENV ?= local
OUT ?= .env
ADB ?= adb
EMULATOR ?= emulator
KTLINT ?= ktlint
APP_ID := com.project.solaria_mobile
MAIN_ACTIVITY := $(APP_ID)/.MainActivity
DEBUG_APK := app/build/outputs/apk/debug/app-debug.apk
ADB_DEVICE := $(if $(DEVICE),-s $(DEVICE),)

.DEFAULT_GOAL := help

.PHONY: help vault-config vault-auth extract-env tools-check env doctor build install run launch devices avds emulator test lint android-lint check clean

help: ## Show the available commands
	@awk 'BEGIN {FS = ":.*## "; printf "Usage: make <target>\n\n"} /^[a-zA-Z_-]+:.*## / {printf "  %-12s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

vault-config: ## Clone or update the shared infra-scripts toolkit
	@if [ -d "$(ORG_SCRIPTS_DIR)/.git" ]; then \
		echo "infra-scripts found at $(ORG_SCRIPTS_DIR), updating..."; \
		git -C "$(ORG_SCRIPTS_DIR)" pull --ff-only || { echo "error: 'git pull --ff-only' failed in $(ORG_SCRIPTS_DIR). Resolve manually, then run 'make vault-config' again."; exit 1; }; \
	elif [ -e "$(ORG_SCRIPTS_DIR)" ]; then \
		echo "error: $(ORG_SCRIPTS_DIR) exists but is not a git clone. Remove or rename it, then run 'make vault-config' again."; exit 1; \
	else \
		echo "infra-scripts not found, cloning into $(ORG_SCRIPTS_DIR)..."; \
		git clone "$(ORG_SCRIPTS_REPO)" "$(ORG_SCRIPTS_DIR)" || { echo "error: failed to clone $(ORG_SCRIPTS_REPO). Check your network/access, then run 'make vault-config' again."; exit 1; }; \
	fi
	@test -f "$(EXTRACT_ENV)" || { echo "error: infra-scripts was cloned/updated but $(EXTRACT_ENV) is missing. Check if the script was renamed or moved upstream."; exit 1; }
	@echo "OK: infra-scripts ready at $(ORG_SCRIPTS_DIR)"

vault-auth: vault-config ## Check the Infisical CLI is installed and authenticated
	@command -v infisical >/dev/null 2>&1 || { echo "error: Infisical CLI not installed. Install it (https://infisical.com/docs/cli/overview), then run 'make vault-auth' again."; exit 1; }
	@infisical user get token --silent >/dev/null 2>&1 || { \
		echo "error: no active Infisical session."; \
		echo "Run: infisical login"; \
		echo "Then run 'make extract-env' again."; \
		exit 1; \
	}
	@echo "OK: Infisical authenticated."

extract-env: vault-auth ## Generate the mobile environment file (ENV=local OUT=.env)
	@test -n "$(SERVICE)" || { echo "error: SERVICE not set. Example: make extract-env SERVICE=mobile-app"; exit 1; }
	@case "$(ENV)" in local|qa|prod) : ;; *) echo "error: invalid ENV '$(ENV)'. Use local, qa or prod (example: make extract-env ENV=qa)"; exit 1;; esac
	$(ORG_SCRIPTS_POWERSHELL) -NoProfile -ExecutionPolicy Bypass -File "$(EXTRACT_ENV)" -Service "$(SERVICE)" -Environment "$(ENV)" -OutputPath "$(OUT)"

tools-check: vault-config ## Alias for vault-config (kept for backwards compatibility)
env: extract-env ## Alias for extract-env (kept for backwards compatibility)

doctor: ## Check the command-line dependencies
	@command -v java >/dev/null 2>&1 || { echo "error: java was not found (JDK 21 is required)"; exit 1; }
	@command -v $(ADB) >/dev/null 2>&1 || { echo "error: adb was not found (install Android SDK Platform-Tools)"; exit 1; }
	@echo "Required Android command-line dependencies are available."

build: ## Build the debug APK
	$(GRADLEW) assembleDebug

install: build ## Build and install the debug app on a connected device
	$(ADB) $(ADB_DEVICE) install -r $(DEBUG_APK)

run: install ## Install and open the app on a connected device
	$(ADB) $(ADB_DEVICE) shell am start -n $(MAIN_ACTIVITY)

launch: ## Open an already installed app
	$(ADB) $(ADB_DEVICE) shell am start -n $(MAIN_ACTIVITY)

devices: ## List connected Android devices and emulators
	$(ADB) devices -l

avds: ## List the available Android virtual devices
	@command -v $(EMULATOR) >/dev/null 2>&1 || { echo "error: emulator was not found (install Android SDK Emulator)"; exit 1; }
	$(EMULATOR) -list-avds

emulator: ## Start an AVD in the foreground (usage: make emulator AVD=name)
	@command -v $(EMULATOR) >/dev/null 2>&1 || { echo "error: emulator was not found (install Android SDK Emulator)"; exit 1; }
	@test -n "$(AVD)" || { echo "error: set AVD, for example: make emulator AVD=Pixel_8_API_35"; exit 1; }
	$(EMULATOR) @$(AVD)

test: ## Run debug unit tests
	$(GRADLEW) testDebugUnitTest

lint: ## Run Kotlin formatting checks (requires ktlint 1.4.1)
	@command -v $(KTLINT) >/dev/null 2>&1 || { echo "error: ktlint was not found (version 1.4.1 is required)"; exit 1; }
	$(KTLINT) "app/src/**/*.kt"

android-lint: ## Run Android lint for the debug variant
	$(GRADLEW) lintDebug

check: test lint android-lint ## Run the local validation suite

clean: ## Remove generated build files
	$(GRADLEW) clean
