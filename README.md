# Online Tender Management System

A web-based Online Tender Management System developed using Java, MySQL, HTML, CSS, and JavaScript.

## 📌 Project Overview

The Online Tender Management System is designed to simplify the process of publishing tenders, managing vendors, approving vendor registrations, and submitting bids through a centralized web application.

## 🚀 Features

### Admin
- Admin login
- Vendor management
- Approve or reject vendor registrations
- Create and publish tenders
- View tender and bid statistics
- Monitor vendor and tender information

### Vendor
- Vendor registration
- Admin approval system
- Secure login
- View published tenders
- Submit bids
- Track submitted bid information

### Visitor
- Browse published tenders
- View tender details
- No login required

## 🛠️ Technologies Used

- Java
- Java HTTP Server
- MySQL
- JDBC
- HTML5
- CSS3
- JavaScript
- MySQL Workbench
- Visual Studio Code

## 🗄️ Database

The system uses MySQL with the following main tables:

- users
- vendors
- tenders
- bids
- activity_logs

## 📂 Project Structure

```text
Online-Tender-Management-System
│
├── src
│   ├── Main.java
│   ├── DBConnection.java
│   ├── LoginHandler.java
│   ├── DashboardHandler.java
│   ├── VendorHandler.java
│   ├── VendorRegistrationHandler.java
│   ├── VendorApprovalHandler.java
│   ├── TenderHandler.java
│   └── BidHandler.java
│
├── lib
│   └── MySQL JDBC Connector
│
└── web
    ├── index.html
    ├── login.html
    ├── admin-dashboard.html
    ├── manage-vendors.html
    ├── manage-tenders.html
    ├── vendor-register.html
    ├── vendor-dashboard.html
    └── vendor-tenders.html
