#!/bin/sh
# This script is for cloud platforms like Railway to know how to build the project
# Build the entire multi-module project
mvn clean package -DskipTests
