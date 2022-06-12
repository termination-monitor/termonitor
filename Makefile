include ./Makefile.os
include ./Makefile.docker
include ./Makefile.maven

PROJECT_NAME ?= termonitor
GITHUB_VERSION ?= main
RELEASE_VERSION ?= latest

ifneq ($(RELEASE_VERSION),latest)
  GITHUB_VERSION = $(RELEASE_VERSION)
endif

.PHONY: release
release: release_prepare release_maven release_version release_pkg

release_prepare:
	rm -rf ./$(PROJECT_NAME)-$(RELEASE_VERSION)
	rm -f ./$(PROJECT_NAME)-$(RELEASE_VERSION).tar.gz
	rm -f ./$(PROJECT_NAME)-$(RELEASE_VERSION).zip
	mkdir ./$(PROJECT_NAME)-$(RELEASE_VERSION)

release_version:
	echo "Changing Docker image tags in install to :$(RELEASE_VERSION)"
	$(FIND) ./packaging/install -name '*.yaml' -type f -exec $(SED) -i '/image: "\?ghcr.io\/termination-monitor\/[a-zA-Z0-9_.-]\+:[a-zA-Z0-9_.-]\+"\?/s/:[a-zA-Z0-9_.-]\+/:$(RELEASE_VERSION)/g' {} \;

release_maven:
	echo "Update pom versions to $(RELEASE_VERSION)"
	mvn $(MVN_ARGS) versions:set -DnewVersion=$(shell echo $(RELEASE_VERSION) | tr a-z A-Z)
	mvn $(MVN_ARGS) versions:commit

release_pkg:
	$(CP) -r ./packaging/install ./
	$(CP) -r ./packaging/install ././$(PROJECT_NAME)-$(RELEASE_VERSION)/
	tar -z -cf ./$(PROJECT_NAME)-$(RELEASE_VERSION).tar.gz $(PROJECT_NAME)-$(RELEASE_VERSION)/
	zip -r ./$(PROJECT_NAME)-$(RELEASE_VERSION).zip $(PROJECT_NAME)-$(RELEASE_VERSION)/
	rm -rf ./$(PROJECT_NAME)-$(RELEASE_VERSION)

.PHONY: all
all: java_package docker_build docker_push

.PHONY: clean
clean: java_clean
