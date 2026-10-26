# FoodBridge — Excess Food Wastage Prevention Platform

**FoodBridge** is a dual-interface (Pure Java AWT GUI & Terminal Console) desktop application engineered to eliminate commercial food wastage by bridging surplus food publishers (Restaurants, Hotels, Banquets, Caterers) with food claimers (NGO's, Orphanages, Relief Shelters).

The technical core of the system combines **geospatial Haversine proximity matching** (which sorts listings nearest-first and flags those within a 10 km radius with an **`[IN RANGE]`** indicator) with **atomic race-condition-safe claims** executed under ACID isolation.

---

## Technical Stack & Architecture

- **Core Runtime:** Java Standard Edition (JDK 17+)
- **Desktop GUI:** Pure `java.awt` and `java.awt.event` (Strictly zero Swing, zero JavaFX, zero third-party GUI frameworks)
- **Terminal UI:** ANSI-formatted Java Console UI (`ConsoleUI`, `ConsoleTable`, `InputValidator`)
- **Persistence:** JDBC with strictly parameterized `PreparedStatement`s (zero SQL string concatenation, preventing SQL injection)
- **Security:** Salted SHA-256 password hashing (16-byte `SecureRandom` salt, constant-time `MessageDigest` comparison, legacy plain-text auto-upgrade)
- **Database Support:** Centralized Cloud MySQL (AWS RDS, GCP Cloud SQL, Azure, Aiven) with automatic embedded relational fallback (H2 in MySQL mode) for instant zero-setup evaluation
- **Architecture Pattern:** Clean Layered Architecture (`model` &rarr; `dao` &rarr; `service` &rarr; `gui` / `ui`)

```text
+-----------------------------------------------------------------------+
|                          PRESENTATION LAYER                           |
|  com.foodbridge.gui (Pure AWT GUI)   |  com.foodbridge.ui (Console UI)|
|  - MainFrame (CardLayout, 1000x650)  |  - ConsoleUI                   |
|  - LoginPanel, RestaurantPanel       |  - ConsoleTable                |
|  - NgoPanel, AwtTablePanel           |  - InputValidator              |
|  - HeaderPanel, StatusBar, Dialogs   |                                |
+-----------------------------------+-----------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------+
|                            SERVICE LAYER                              |
|  com.foodbridge.service                                               |
|  - MatchingService (Haversine formula, 10km threshold sorting)        |
|  - AuthService (User registration, authentication, salted hashing)   |
|  - FoodListingService (Create, update, cancel, expiry sweep)          |
|  - ClaimService (Atomic race-condition-safe claiming, pickup history) |
|  - PasswordHasher (Salted SHA-256 & transparent migration)           |
+-----------------------------------+-----------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------+
|                       DATA ACCESS LAYER (DAO)                         |
|  com.foodbridge.dao                                                   |
|  - UserDAO, FoodListingDAO, ClaimDAO, LocationDAO                    |
|  - NotificationDAO (Real-time in-app user alerts)                    |
|  - ActivityLogDAO (Audit log of platform events)                      |
+-----------------------------------+-----------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------+
|                          PERSISTENCE LAYER                            |
|  com.foodbridge.config                                                |
|  - DBConnection (JDBC Manager, connection testing, mode switching)    |
|  - DatabaseSetup (DDL initialization, schema migration, 50 locations) |
|  ENGINES: Cloud MySQL (Remote Multi-User) <--> Embedded H2 (Local)   |
+-----------------------------------------------------------------------+
```

---

## Graphical User Interface Screens

The Pure AWT desktop GUI (`MainFrame`, 1000x650) provides responsive views powered by our enhanced `AwtTablePanel`:
- **Login & Registration (`LoginPanel`):** Role selection (NGO / Restaurant), 50-locality base selector, and demo one-click shortcuts.
- **Restaurant Dashboard (`RestaurantPanel`):** Surplus food publisher form with deadline picker, plus the **"My Food Listings"** table (9 columns) with live claim status and cancellation actions.
- **NGO / Food Claimer Portal (`NgoPanel`):** **"Nearby Food Listings"** table sorted nearest-first via Haversine distance with **`[IN RANGE]`** tags and atomic race-safe claiming; **"My Claimed Pickups"** table with restaurant contact and pickup details.
- **Enhanced `AwtTablePanel` Capabilities:**
  - **Resizable Columns:** Interactive column header dividers dragged with mouse listeners (`MouseMotionListener`) displaying `Cursor.E_RESIZE_CURSOR` (50px minimum width).
  - **Double-Click Auto-Fit:** Double-clicking any header divider auto-fits the column to its longest text using `FontMetrics`.
  - **Content-Aware Default Widths:** Computes optimal widths on first display using `FontMetrics` so long text (*Food Name*, *Pickup Site*, *Claimed By*) is immediately readable.
  - **Synchronized Horizontal Scrolling:** Dynamic `ScrollPane` horizontal scrollbar appears when total width exceeds the viewport, keeping sticky headers and data rows in lockstep alignment.
  - **Ellipsis Truncation & Hover Tooltip:** Overflowing cell text is clipped with `...` and reveals full text in a lightweight AWT hover popup window.
  - **Seamless Row Highlighting:** Selected and hovered rows render as one continuous full-width background rectangle without column gaps or vertical stripe artifacts.

---

## Clean Project Structure

```text
FoodBridge/
├── src/                          # Java source code (clean layered packages)
│   └── com/foodbridge/
│       ├── config/               # DBConnection, DatabaseSetup, DemoReset
│       ├── dao/                  # UserDAO, FoodListingDAO, ClaimDAO, etc.
│       ├── gui/                  # Pure AWT GUI (MainFrame, Panels, Theme)
│       ├── model/                # User, FoodListing, Claim, Location, etc.
│       ├── service/              # MatchingService, AuthService, ClaimService
│       ├── test/                 # SystemVerificationTest (67 automated tests)
│       ├── ui/                   # Terminal console user interface
│       └── Main.java             # Terminal console entry point
├── lib/                          # External JDBC driver JARs
│   ├── h2-2.3.232.jar            # Embedded relational database engine
│   └── mysql-connector-j-8.4.0.jar # Official MySQL JDBC Connector
├── sql/
│   └── schema.sql                # Complete relational DDL script
├── docs/                         # Documentation, Diagrams & Viva Defense
│   ├── Project_Report.docx       # Formal 4-page Academic Engineering Report
│   ├── Project_Report.pdf        # Verified 4-page PDF version
│   ├── VIVA_NOTES.md             # Comprehensive Viva & Defense study guide
│   └── diagrams/                 # Editable .drawio files & high-res PNGs
│       ├── use_case_diagram.drawio & .png
│       ├── dfd_level0_level1.drawio & .png
│       ├── class_diagram.drawio & .png
│       ├── sequence_diagram_claim.drawio & .png
│       └── pipeline_flow.drawio & .png
├── Execute.bat                   # ONE-CLICK setup, compilation & GUI launcher
├── Execute.ps1                   # Companion PowerShell one-click launcher
├── compile.bat / compile.ps1     # Build scripts (compiles into bin/)
├── run.bat / run.ps1             # Application launcher (GUI or Console mode)
├── reset_demo.bat / reset_demo.ps1 # Demo state reset & seed utility
├── db.properties.example         # Template configuration with placeholders
├── .gitignore                    # Excludes build output, archives & secrets
└── README.md                     # System documentation
```

---

## How to Build & Run

### 1. One-Click Execution (Recommended)
Simply double-click **`Execute.bat`**.

`Execute.bat` automatically:
1. Validates that `java` and `javac` (JDK 17+) exist on your system.
2. Checks for `db.properties`; if missing, automatically initializes it from `db.properties.example` (running in zero-setup embedded H2 mode).
3. Compiles the Java source files into `bin/`.
4. Launches the AWT Graphical User Interface (`MainFrame`).

### 2. Manual Command-Line (Windows Batch)
```cmd
:: 1. Compile
compile.bat

:: 2. Launch Graphical Interface (GUI Mode)
run.bat gui

:: 3. Launch Terminal Console Mode
run.bat console

:: 4. Interactive Interface Selector
run.bat
```

### 3. PowerShell Route (Execution Policy Safe)
If running under Windows PowerShell, use `-ExecutionPolicy Bypass`:
```powershell
# One-click launch
powershell -ExecutionPolicy Bypass -File .\Execute.ps1

# Manual compile & launch
powershell -ExecutionPolicy Bypass -File .\compile.ps1
powershell -ExecutionPolicy Bypass -File .\run.ps1 -Mode gui
powershell -ExecutionPolicy Bypass -File .\run.ps1 -Mode console
```

---

## Evaluator Quick-Login Credentials

For fast demonstration, the login screen includes one-click demo shortcuts:

| Demo Role | Button Label | Username | Password | Location |
| :--- | :--- | :--- | :--- | :--- |
| **Food Publisher** | `Demo Restaurant` | `demo_restaurant_1` | `Password@123` | Multi-site caterer (pickup per listing) |
| **Food Claimer** | `Demo NGO` | `demo_ngo` | `Password@123` | Guindy (`13.0067, 80.2026`) |

*Note: New accounts can also be freely registered using the in-app "Create an account" toggle.*

---

## Resetting Demo State (`reset_demo.bat`)

To clean up test claims and restore fresh sample data for evaluators:
```cmd
reset_demo.bat
```
*(or with PowerShell: `powershell -ExecutionPolicy Bypass -File .\reset_demo.ps1`)*

- **Displays Active DB:** Prints target engine (`MySQL` or `Embedded H2`) and target JDBC URL.
- **Safety Confirmation:** Requires typing `"yes"` before deleting any records.
- **FK-Safe Teardown:** Clears transactional tables in relational order:
  $$\text{claims} \longrightarrow \text{notifications} \longrightarrow \text{activity\_log} \longrightarrow \text{food\_listings}$$
- **Data Preservation:** Keeps all registered user accounts and all 50 reference Chennai locations intact.
- **Fresh Seed Data:** Inserts 5 fresh `AVAILABLE` sample food listings with deadlines 2–4 days in the future across Chennai localities (Velachery, Guindy, Saidapet, T. Nagar, Tambaram).

---

## Connecting to a Shared Cloud Database

To demonstrate multi-user collaborative workflows across two physical laptops (e.g. Laptop A posts as a Restaurant, Laptop B claims as an NGO):

1. **Obtain Database Credentials:** Get the remote MySQL credentials provided by your team lead.
2. **Configure `db.properties`:**
   - Copy `db.properties.example` to `db.properties`.
   - Update `db.properties` with the cloud connection details:
     ```properties
     db.mode=mysql
     mysql.driver=com.mysql.cj.jdbc.Driver
     mysql.url=jdbc:mysql://YOUR_CLOUD_HOST:3306/foodbridge?useSSL=true&serverTimezone=UTC
     mysql.user=YOUR_CLOUD_USERNAME
     mysql.password=YOUR_CLOUD_PASSWORD
     fallback.to.embedded=true
     ```
3. **Launch on Both Machines:** Run `Execute.bat` on both laptops. Both instances will connect to the same cloud database in real time.

---

## Mathematical Formulation: Haversine Geospatial Matching

Distance between the requesting NGO's base coordinates and listing pickup coordinates is calculated in `MatchingService` using the **Haversine formula**:

$$\Delta\text{lat} = \text{radians}(\text{lat}_2 - \text{lat}_1)$$
$$\Delta\text{long} = \text{radians}(\text{long}_2 - \text{long}_1)$$
$$a = \sin^2\left(\frac{\Delta\text{lat}}{2}\right) + \cos(\text{lat}_1) \cdot \cos(\text{lat}_2) \cdot \sin^2\left(\frac{\Delta\text{long}}{2}\right)$$
$$c = 2 \cdot \text{atan2}\left(\sqrt{a}, \sqrt{1 - a}\right)$$
$$\text{distance} = R \cdot c \quad (R = 6371.0 \text{ km})$$

- **10km Threshold:** Listings where $\text{distance} \le 10.0\text{ km}$ receive the **`[IN RANGE]`** highlight.
- **Full Situational Visibility:** Listings farther than 10 km remain visible below so NGOs can coordinate vehicle pickup if needed.

---

## Predefined Reference Data: 50 Real Localities

The system comes pre-seeded with **50 real-world localities** across the Chennai and Tamil Nadu corridor:
1. Kanchipuram (12.8342, 79.7036)
2. Guindy (13.0067, 80.2026)
3. Tambaram (12.9249, 80.1000)
4. Chrompet (12.9516, 80.1462)
5. Velachery (12.9815, 80.2180)
6. T. Nagar (13.0418, 80.2341)
7. Anna Nagar (13.0850, 80.2101)
8. Porur (13.0382, 80.1565)
9. Sriperumbudur (12.9667, 79.9400)
10. Adyar (13.0012, 80.2565)
*(and 40 more localities including Mylapore, Saidapet, Koyambedu, Ambattur, Chengalpattu, Walajabad, etc.)*

---

## Automated Verification Suite

Run the full automated test suite (67 unit & integration tests covering distance calculations, password hashing, legacy migration, race-condition safety, notifications, and audit logging):
```cmd
java -cp "bin;lib/*" com.foodbridge.test.SystemVerificationTest
```
All **67 / 67 automated assertions pass cleanly**.
