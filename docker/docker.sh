#!/bin/bash
FULL_PATH="$PWD"
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m'

function create_machine {
	echo -e "${RED}Default machine with virtualbox driver does not exist.${NC}"
	echo -e "${GREEN}Creating a default machine with the virtualbox driver...${NC}"
	
	docker-machine create --driver virtualbox default
	
	echo -e "${GREEN}Created machine.${NC}"
}

function start_machine {
	echo -e "${RED}The default machine is not running.${NC}"
	echo -e "${GREEN}Starting default machine...${NC}"
	
	docker-machine start
	
	echo -e "${GREEN}Setting environment variables for the machine${NC}"
	
	eval "$(docker-machine env default)"
	
	echo -e "${GREEN}Started default machine.${NC}"
}

function build_image {
	echo -e "${RED}Docker image 'docker-nrf5' not created.${NC}"
	echo -e "${GREEN}Creating 'docker-nrf5' image...${NC}"
	
	docker build --no-cache -t docker-nrf5 .
	
	echo -e "${GREEN}Created Docker image 'docker-nrf5'.${NC}"
}

if [[ $(docker-machine ls | grep -c "default.*virtualbox") -eq 0 ]]; then
	create_machine
fi

if [[ $(docker-machine ls | grep -c "default.*virtualbox.*Running") -eq 0 ]]; then
	start_machine
fi

# Check if docker-nrf5 image is created
if [[ $(docker images | grep -c "docker-nrf5") -eq 0 ]]; then
    build_image
fi

if [[ ${FULL_PATH##/*/} != "docker" ]]; then
    echo -e "${RED}Error: Rerun docker.sh inside the docker directory of the project's root!${NC}"
    exit 1
else
    # Remove "docker" from FULL_PATH. Equivalent to "cd .."
    FULL_PATH=${FULL_PATH%/*}
fi

eval "$(docker-machine env default)"

docker run -ti --rm --name nrf5 --privileged -v /dev/ttyUSB0:/dev/ttyUSB0 --mount type=bind,source=${FULL_PATH},target=/smart-pinguins docker-nrf5 /bin/bash
#docker run -ti --rm --name nrf5 --device /dev/ttyUSB0:/dev/ttyUSB0 -v ${FULL_PATH}:/smart-pinguins docker-nrf5 /bin/bash

# Open new connection to container
#docker exec -ti nrf5 /bin/bash
