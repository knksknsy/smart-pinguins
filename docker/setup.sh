#!/bin/bash
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m'

function main {
    install_tools
    install_toolchain
    install_nrf_cli
    install_segger
    install_nrf5_sdk
    install_python_dependencies
    setup_build_environments

    echo "${GREEN}Docker image 'docker-nrf5' created.${NC}"
}

function fn_cd {
    cd $1
}

function install_tools {
    echo "${GREEN}Installing tools...${NC}"
    echo ""

    echo `wget http://mirrors.kernel.org/ubuntu/pool/main/d/device-tree-compiler/device-tree-compiler_1.4.7-1_amd64.deb`
    echo `dpkg -i device-tree-compiler_1.4.7-1_amd64.deb`
    echo `rm -rf device-tree-compiler_1.4.7-1_amd64.deb`

    echo `pip3 install --user cmake`

    echo "${GREEN}Tools installed.${NC}"
    echo ""
}

function install_toolchain {
    echo "${GREEN}Installing GNU Embedded Toolchain for Arm...${NC}"
    echo ""

    echo `wget https://launchpad.net/gcc-arm-embedded/4.9/4.9-2015-q3-update/+download/gcc-arm-none-eabi-4_9-2015q3-20150921-linux.tar.bz2`
    echo `tar -xvf gcc-arm-none-eabi-4_9-2015q3-20150921-linux.tar.bz2`
    # echo `rm -rf gcc-arm-none-eabi-4_9-2015q3-20150921-linux.tar.bz2`

    echo "${GREEN}GNU Embedded Toolchain for Arm installed.${NC}"
    echo ""
}

function install_nrf_cli {
    echo "${GREEN}Installing nRF-Command-Line-Tools...${NC}"
    echo ""

    echo `wget https://nordicsemi.com/-/media/Software-and-other-downloads/Desktop-software/nRF-command-line-tools/sw/Versions-10-x-x/nRFCommandLineTools1050Linuxamd64tar.gz`
    echo `tar -xvf nRFCommandLineTools1050Linuxamd64tar.gz`
    echo `rm -rf nRFCommandLineTools1050Linuxamd64tar.gz`
    
    echo `tar -xvf nRF-Command-Line-Tools_10_5_0_Linux-amd64.tar.gz`
    echo `rm -rf nRF-Command-Line-Tools_10_5_0_Linux-amd64.tar.gz`

    echo `dpkg -i nRF-Command-Line-Tools_10_5_0_Linux-amd64.deb`
    echo `rm -rf nRF-Command-Line-Tools_10_5_0_Linux-amd64.deb`

    echo "${GREEN}nRF-Command-Line-Tools installed.${NC}"
    echo ""
}

function install_segger {
    echo "${GREEN}Installing Segger J-Link Software...${NC}"
    echo ""

    echo `tar -xzf JLink_Linux_V654c_x86_64.tgz`
    echo `rm -rf JLink_Linux_V654c_x86_64.tgz`

    echo `dpkg -i JLink_Linux_V654c_x86_64.deb`
    echo `rm -rf JLink_Linux_V654c_x86_64.deb`

    echo "${GREEN}Segger J-Link Software installed.${NC}"
    echo ""
}

function install_nrf5_sdk {
    echo "${GREEN}Installing nRF Connect SDK...${NC}"
    echo ""

    echo `mkdir -p /ncs`
    fn_cd "/ncs"

    echo `pip3 install --upgrade pip`
    echo `pip3 install --user west`

    echo `west init -m https://github.com/NordicPlayground/fw-nrfconnect-nrf`
    echo `west update`

    echo "${GREEN}nRF Connect SDK installed.${NC}"
    echo ""
}

function install_python_dependencies {
    echo "${GREEN}Installing additional Python dependencies...${NC}"
    echo ""
    
    echo `pip3 install --user -r zephyr/scripts/requirements.txt`
    echo `pip3 install --user -r nrf/scripts/requirements.txt`
    echo `pip3 install --user -r mcuboot/scripts/requirements.txt`

    echo "${GREEN}Additional Python dependencies installed.${NC}"
    echo "" 
}

function setup_build_environments {
    echo "${GREEN}Setting up the build environments...${NC}"
    echo ""

    echo `source zephyr/zephyr-env.sh`

    echo "${GREEN}Build environments set up.${NC}"
    echo ""
}

main