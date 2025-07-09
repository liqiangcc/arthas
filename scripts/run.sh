#!/bin/bash
# Arthas Run Script
# Usage: ./run.sh

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ARTHAS_HOME="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="$ARTHAS_HOME/packaging/target/arthas-bin"
ARTHAS_BOOT_JAR="$BUILD_DIR/arthas-boot.jar"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}        Arthas Run Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo

# Check Java Environment
echo "Checking Java environment..."
if ! command -v java &> /dev/null; then
    echo -e "${RED}ERROR: Java not found, please ensure Java is installed and in PATH${NC}"
    exit 1
fi
echo -e "${GREEN}SUCCESS: Java environment check passed${NC}"
echo

# Check Build Artifacts
echo "Checking build artifacts..."
if [ ! -d "$BUILD_DIR" ]; then
    echo -e "${RED}ERROR: Build directory does not exist: $BUILD_DIR${NC}"
    echo -e "${YELLOW}Please run ./build.sh first to build the project${NC}"
    exit 1
fi

if [ ! -f "$ARTHAS_BOOT_JAR" ]; then
    echo -e "${RED}ERROR: Arthas Boot JAR does not exist: $ARTHAS_BOOT_JAR${NC}"
    echo -e "${YELLOW}Please run ./build.sh first to build the project${NC}"
    exit 1
fi

echo -e "${GREEN}SUCCESS: Build artifacts check passed${NC}"
echo "  - Arthas Boot JAR: $ARTHAS_BOOT_JAR"
echo

echo "Starting Arthas Boot..."
echo "You can now attach to any Java process or start a new one."
echo
echo -e "${BLUE}==================== Arthas Boot ====================${NC}"

# Run Arthas Boot
java -jar "$ARTHAS_BOOT_JAR"

echo
echo -e "${BLUE}=====================================================${NC}"
echo
echo "Arthas Boot has exited."
echo -e "${GREEN}Script execution completed!${NC}"
