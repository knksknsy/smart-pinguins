#!/bin/bash

if hash git \
    && hash pip3 \
    && hash arm-none-eabi-gcc \
    && hash nrfjprog \
    && hash JLinkExe \
    && [ -d "$GNUARMEMB_TOOLCHAIN_PATH" ]; then
    echo 'Needed software is installed.'
else
    exit 1
fi
