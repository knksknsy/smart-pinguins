FROM ubuntu:16.04

LABEL maintainer="Kaan Keskinsoy kk148@hdm-stuttgart.de"

ENV PATH="$PATH:$HOME/"
ENV ZEPHYR_TOOLCHAIN_VARIANT=gcc-arm-none-eabi-4_9-2015q3
ENV GNUARMEMB_TOOLCHAIN_PATH="$PATH:/gcc-arm-none-eabi-4_9-2015q3"
ENV PATH="$PATH:/usr/bin/python3"
ENV PATH="$PATH:/JLink_Linux_V654c_x86_64"
ENV PATH="$PATH:/nrfjprog"
ENV PATH="$PATH:~/.local/bin"

COPY ./setup.sh /
COPY ./JLink_Linux_V654c_x86_64.tgz /

RUN DEBIAN_FRONTEND=nointeractive apt-get update -qq \
    && DEBIAN_FRONTENT=nointeractive apt-get upgrade -qq \
    && DEBIAN_FRONTENT=nointeractive apt-get install -yq \
        sudo \
        git \
        wget \
        unzip \
        cmake \
        ninja-build \
        gperf \
        ccache \
        dfu-util \
        device-tree-compiler \
        nano \
        python3-pip \
        python3-setuptools \
        python3-wheel \
        xz-utils \
        file \
        make \
        gcc \
        gcc-multilib

RUN chmod +x /setup.sh
RUN /setup.sh
