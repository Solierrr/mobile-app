SHELL := /bin/sh

GRADLEW := ./gradlew
ORG_SCRIPTS_DIR ?= $(HOME)/.local/share/solierrr-infra-scripts
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

.PHONY: help tools-check env doctor build install run launch devices avds emulator test lint android-lint check clean

help: ## Show the available commands
	@awk 'BEGIN {FS = ":.*## "; printf "Usage: make <target>\n\n"} /^[a-zA-Z_-]+:.*## / {printf "  %-12s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

tools-check: ## Verify that the shared organization scripts are installed
	@test -f "$(EXTRACT_ENV)" || { echo "error: infra-scripts was not found at $(ORG_SCRIPTS_DIR)"; exit 1; }

env: tools-check ## Generate the mobile environment file (ENV=local OUT=.env)
	$(ORG_SCRIPTS_POWERSHELL) -NoProfile -ExecutionPolicy Bypass -File "$(EXTRACT_ENV)" -Service "$(SERVICE)" -Environment "$(ENV)" -OutputPath "$(OUT)"

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
