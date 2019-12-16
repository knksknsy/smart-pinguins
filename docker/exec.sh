#!/bin/bash

eval "$(docker-machine env default)"

docker exec -ti nrf5 /bin/bash
