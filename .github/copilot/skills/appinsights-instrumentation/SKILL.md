---
name: appinsights-instrumentation
description: 'Instrument a webapp to send useful telemetry data to Azure App Insights'
---

# AppInsights instrumentation

This skill enables sending telemetry data of a webapp to Azure App Insights for better observability of the app's health.

## When to use this skill

Use this skill when the user wants to enable telemetry for their webapp.

## Prerequisites

The app in the workspace must be one of these kinds:
- An ASP.NET Core app hosted in Azure
- A Node.js app hosted in Azure

## Guidelines

### Collect context information

Find out the (programming language, application framework, hosting) tuple. Read the source code to make an educated guess. Confirm with the user on anything you don't know. Ask where the application is hosted (e.g. on a personal computer, in an Azure App Service as code, in an Azure App Service as container, in an Azure Container App, etc.).

### Prefer auto-instrument if possible

If the app is a C# ASP.NET Core app hosted in Azure App Service, use AUTO guide to help user auto-instrument the app.

### Manually instrument

Manually instrument the app by creating the AppInsights resource and updating the app's code.

#### Create AppInsights resource

Use one of the following options:
- Add AppInsights to existing Bicep template
- Use Azure CLI

#### Modify application code

- If the app is an ASP.NET Core app, use the ASPNETCORE guide
- If the app is a Node.js app, use the NODEJS guide
- If the app is a Python app, use the PYTHON guide
