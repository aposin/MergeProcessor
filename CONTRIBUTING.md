# Workspace Setup

## Introduction
This repository uses Oomph to setup the workspace for contribution. It is open to the developer to setup the workspace on its own. The Oomph setup 
process triggers the following tasks: 
* Download and setup the required Eclipse IDE.
* Clone the repository
* Checkout the existing projects
* Download and activate required target platform
* Define default working sets

## System requirements
* Microsoft Windows 64 Bit
* Apache Maven (tested with 3.5.2)
* GIT (tested with 2.15.0.windows.1)
* Java Development Kit (tested with Oracle JDK 1.8.0_171)

## How to setup with Oomph
* Download the Eclipse installer from `https://www.eclipse.org/downloads/packages/installer`
* Start the Eclipse installer
* Switch to the `Advanced Mode`
* Add a new product configuration: download from `https://github.com/aposin/MergeProcessor/blob/master/products/org.aposin.mergeprocessor.product/mergeprocessor.product.setup`
* Select the added prodcuct configuration and click next.
* Add a new project configuration: download from `https://github.com/aposin/MergeProcessor/blob/master/products/org.aposin.mergeprocessor.product/mergeprocessor.project.setup`
* Select the added project configuration and click next.
* Edit or change the variables (optional) and click next.
* Finish the setup configurations start the setup process by clicking finish.
* Wait till the Eclipse and the workspace are setup successfully.
* Run mergeprocessor_mvn_package to check the maven build is working correctly.
* Run mergeprocessor.product to start the application. 