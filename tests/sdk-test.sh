#!/bin/bash

if hash git \
    && hash pip3 \
    && hash /gcc-arm-none-eabi-4_9-2015q3/bin/arm-none-eabi-gcc \
    && hash nrfjprog \
    && hash JLinkExe; then
    echo 'Needed software is installed.'
else
    exit 1
fi
