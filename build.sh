#!/bin/bash

set -e

echo "=== Starting Crypto Core Build ==="

if [ ! -d "build" ]; then
  mkdir build
fi

cd build

echo "Configuring project with CMake..."
cmake ../crypto-core

echo "Compiling the static library..."
cmake --build .

echo "=== Build Successful! ==="
echo "The static library is located in the 'build' directory."
