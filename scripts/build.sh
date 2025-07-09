#!/bin/bash
# Arthas Build Script
# Usage: ./build.sh

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
echo -e "${BLUE}        Arthas Build Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo

# Step 1: Check Java Environment
echo -e "${YELLOW}[Step 1/2] Checking Java environment...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}ERROR: Java not found, please ensure Java is installed and in PATH${NC}"
    exit 1
fi
echo -e "${GREEN}SUCCESS: Java environment check passed${NC}"
echo

# Step 2: Build Project
echo -e "${YELLOW}[Step 2/2] Building Arthas project...${NC}"
cd "$ARTHAS_HOME"

echo "Executing: ./mvnw clean install -DskipTests -pl common,spy,core,agent,client,boot,packaging -am"
./mvnw clean install -DskipTests -pl common,spy,core,agent,client,boot,packaging -am
if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Project build failed${NC}"
    exit 1
fi
echo -e "${GREEN}SUCCESS: Project build completed${NC}"
echo

# Check Build Artifacts
echo "Checking build artifacts..."
if [ ! -d "$BUILD_DIR" ]; then
    echo -e "${RED}ERROR: Build directory does not exist: $BUILD_DIR${NC}"
    exit 1
fi

if [ ! -f "$ARTHAS_BOOT_JAR" ]; then
    echo -e "${RED}ERROR: Arthas Boot JAR does not exist: $ARTHAS_BOOT_JAR${NC}"
    exit 1
fi

echo -e "${GREEN}SUCCESS: Build artifacts check passed${NC}"
echo "  - Build Directory: $BUILD_DIR"
echo "  - Arthas Boot JAR: $ARTHAS_BOOT_JAR"
echo

echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Build completed successfully!${NC}"
echo "You can now run: ./run.sh"
echo -e "${BLUE}========================================${NC}"
