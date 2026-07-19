# 🛒 Grocery Shop Management System

A professional, offline desktop application for small village grocery shops.  
Built with **JavaFX 21 + SQLite** — no internet, no database server needed.

---

## Default Login

| Field    | Value      |
|----------|------------|
| Username | `admin`    |
| Password | `admin123` |

> ⚠️ **Change the password immediately** in Settings after first login.

---

## Quick Start (Development)

**Requirements:** Java 21 JDK, Maven 3.x

```bash
# Run the application
mvn javafx:run
```

Or double-click **`run.bat`**

---

## Building for Production

### Option A — Portable App (Recommended, no extra tools needed)
```bash
mvn clean package
build-portable.bat
```
Produces: `portable\GroceryShop\GroceryShop.exe`  
Zip and copy the entire `GroceryShop` folder to the shop computer.

### Option B — Windows Installer (.exe)
Requires [Wix Toolset 3.x](https://wixtoolset.org/releases/) to be installed first.
```bash
mvn clean package
build-installer.bat
```
Produces: `installer\GroceryShop-1.0.0.exe`

---

## Data Storage

All data is stored automatically in:  
`%APPDATA%\GroceryShop\grocery.db`

| Folder                              | Contents              |
|-------------------------------------|-----------------------|
| `%APPDATA%\GroceryShop\`           | Database file         |
| `%APPDATA%\GroceryShop\Backups\`   | Database backups      |
| `%APPDATA%\GroceryShop\Reports\`   | Saved reports         |
| `%APPDATA%\GroceryShop\Logs\`      | Application logs      |

---

## Modules

| Module       | Features                                                  |
|--------------|-----------------------------------------------------------|
| Dashboard    | Today's sales, profit, low stock alerts, recent bills     |
| Billing      | Fast product search, cart, discount, GST, print/PDF       |
| Products     | Add/edit/delete products, barcode, category, supplier     |
| Customers    | Customer database with purchase history                   |
| Suppliers    | Supplier database with product linking                    |
| Inventory    | Stock levels, low-stock alerts, restock (add stock)       |
| Reports      | Daily/Weekly/Monthly/Yearly sales and profit reports      |
| Backup       | One-click backup, restore from file, backup history       |
| Settings     | Shop info, GST, currency, low-stock threshold             |

---

## Project Structure

```
src/main/java/com/grocery/
├── app/          ← Entry point (MainApp, Launcher, SessionManager)
├── controller/   ← JavaFX UI controllers (one per screen)
├── dao/          ← SQLite data access (DatabaseManager + DAOs)
├── model/        ← Data models (POJOs)
├── service/      ← Business logic (Auth, Billing, Backup)
└── util/         ← Utilities (Alert, Print, Currency, Validation)

src/main/resources/
├── fxml/         ← UI layouts (one per screen)
├── css/          ← Dark theme stylesheet
└── images/       ← App icons (optional)
```

---

## Technology

- **Java 21** (OpenJDK / Microsoft Build)
- **JavaFX 21** (UI framework)
- **SQLite** via `sqlite-jdbc` (embedded database, no installation)
- **Maven 3.9** (build tool)
- **jpackage** (packaging to Windows EXE — bundled with JDK 21)

---

## Architecture

MVC pattern with a clean layer separation:
- **View** → FXML files (declarative UI)
- **Controller** → JavaFX controllers (UI logic only)
- **Service** → Business logic (billing, auth, backup)
- **DAO** → Database access (SQLite JDBC)
- **Model** → Plain Java objects

---

*Built for offline use. No internet connection required after initial Maven dependency download.*
