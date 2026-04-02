# Tickify

Tickify is a tertiary event ticketing platform for South African universities and colleges. It is built as a Java EE (JEE) web application and organized into three connected components:

1. Admin Console
2. Client Site
3. Security Scanner

## Overview

Tickify helps campus event teams publish and manage events, while students and external attendees can discover events and buy digital tickets. Each ticket includes a unique QR code used at venue entrances for fast and secure verification.

The platform supports institutions such as UJ, Wits, UP, and UCT, and can enforce student-only event access using institutional email-domain validation.

## Core Components

### 1. Admin Console

Administrators can:

- Create and manage campus events
- Configure student-only restrictions
- Track ticket sales and attendance
- Monitor revenue through a real-time dashboard

### 2. Client Site

Students and external attendees can:

- Browse upcoming campus events
- Register using university credentials
- Purchase event tickets
- Receive unique QR-coded tickets for entry

### 3. Security Scanner

Venue guards use a mobile-first web interface to:

- Scan ticket QR codes via camera
- Validate tickets through manual code entry (fallback)
- Receive instant validation feedback (visual, audio, and vibration)

## Java EE Architecture

This system follows a layered Java EE structure:

- Presentation layer: JSP pages and servlet controllers (controllers call application services, not DAOs directly)
- Application layer: service classes in `za.ac.tut.application.*` for authentication and registration workflows
- Data-access layer: DAO classes in `za.ac.tut.databaseManagement` for entity operations
- Persistence layer: entities and persistence configuration

All client applications communicate with the backend through REST-style API calls. Authentication state is maintained on the client using localStorage session tokens.

## Data Storage and Initialization

The datastore layer supports:

- Local JSON file storage
- Oracle database connectivity

On first launch, the server seeds default administrator and security accounts to simplify initial setup.

## Database Credentials (Required)

Tickify now requires explicit database credentials. No default database password is used.

Set either JVM properties or environment variables before starting the app:

- JVM properties: `-Dtickify.db.user=... -Dtickify.db.password=...`
- Environment variables: `TICKIFY_DB_USER` and `TICKIFY_DB_PASSWORD`

Optional connection settings (if not provided):

- `TICKIFY_DB_HOST` (default: `localhost`)
- `TICKIFY_DB_PORT` (default: `1527`)
- `TICKIFY_DB_NAME` (default: `tickifyDB`)

## Password Reset Email Delivery

Client password reset links are now delivered by email (not shown on-screen).

Configure SMTP via environment variables or JVM properties:

- `TICKIFY_SMTP_HOST` or `-Dtickify.smtp.host=...`
- `TICKIFY_SMTP_PORT` or `-Dtickify.smtp.port=...` (default: `587`)
- `TICKIFY_SMTP_USER` or `-Dtickify.smtp.user=...`
- `TICKIFY_SMTP_PASSWORD` or `-Dtickify.smtp.password=...`
- `TICKIFY_SMTP_FROM` or `-Dtickify.smtp.from=...`
- `TICKIFY_SMTP_STARTTLS` or `-Dtickify.smtp.starttls=true|false` (default: `true`)
- `TICKIFY_SMTP_SSL` or `-Dtickify.smtp.ssl=true|false` (default: `false`)

Branding and link generation settings:

- `TICKIFY_APP_BASE_URL` or `-Dtickify.app.baseUrl=...` for absolute reset links in email.
- `TICKIFY_LOGO_URL` or `-Dtickify.logo.url=...` for the logo rendered in the email template.

## Project Context

This repository contains the Java EE implementation for the web platform, including servlets, DAO classes, entities, and JSP-based views for the different user roles.

## Branding

This project uses the Tickify brand name across the application and documentation.

## Production Hardening

Use [PRODUCTION_HARDENING_CHECKLIST.md](PRODUCTION_HARDENING_CHECKLIST.md) as the release gate checklist for security, validation, scanner readiness, client readiness, admin readiness, and deployment operations.

## Oracle Cloud Free Tier Deployment

Deployment automation for Oracle Cloud Free Tier VM is included in:

- `deploy/oracle-free-tier/setup-vm.sh`
- `deploy/oracle-free-tier/deploy-app.sh`
- `deploy/oracle-free-tier/provision-and-deploy-oci.sh`
- `deploy/oracle-free-tier/terminate-oci-vm.sh`
- `deploy/oracle-free-tier/tickify.service`
- `deploy/oracle-free-tier/nginx-tickify.conf`
- `deploy/oracle-free-tier/tickify.env.example`

Detailed step-by-step guide:

- [docs/ORACLE_CLOUD_FREE_TIER_DEPLOYMENT.md](docs/ORACLE_CLOUD_FREE_TIER_DEPLOYMENT.md)
