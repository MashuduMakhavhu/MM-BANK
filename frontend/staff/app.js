const API_BASE = "https://mm-bank-backend-2ca5.onrender.com/api";

const loginView = document.getElementById("login-view");
const appView = document.getElementById("app-view");
const loginForm = document.getElementById("login-form");
const loginError = document.getElementById("login-error");
const loginButton = document.getElementById("login-button");
const pageContent = document.getElementById("page-content");

let currentStaff = null;

async function getCsrfToken() {
    const response = await fetch(`${API_BASE}/auth/csrf`, {
        method: "GET",
        credentials: "include"
    });

    if (!response.ok) {
        throw new Error("Unable to establish a secure connection.");
    }

    const data = await response.json();

    if (!data.token) {
        throw new Error("CSRF token was not returned by the server.");
    }

    return data.token;
}

async function api(path, options = {}) {
    const method = (options.method || "GET").toUpperCase();

    const config = {
        ...options,
        method,
        credentials: "include",
        headers: {
            ...(options.headers || {})
        }
    };

    if (options.body && !(options.body instanceof FormData)) {
        config.headers["Content-Type"] = "application/json";
    }

    if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
        config.headers["X-XSRF-TOKEN"] = await getCsrfToken();
    }

    const response = await fetch(`${API_BASE}${path}`, config);

    if (response.status === 401) {
        throw new Error("Your session has expired. Please sign in again.");
    }

    if (response.status === 403) {
        throw new Error("Access denied. Your account may not have permission.");
    }

    if (!response.ok) {
        let message = "The request could not be completed.";

        try {
            const data = await response.json();
            message = data.detail || data.message || data.error || message;
        } catch {
            // Keep the default message if the response isn't JSON.
        }

        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }

    const contentType = response.headers.get("content-type") || "";

    if (contentType.includes("application/json")) {
        return response.json();
    }

    return null;
}

function showLogin(message = "") {
    currentStaff = null;
    appView.classList.add("hidden");
    loginView.classList.remove("hidden");
    loginError.textContent = message;
    loginButton.disabled = false;
    loginButton.textContent = "Sign in";
}

function showApplication(staff) {
    currentStaff = staff;

    loginView.classList.add("hidden");
    appView.classList.remove("hidden");

    document.getElementById("staff-identity").textContent =
        `${staff.fullName} · ${staff.staffNumber}`;

    document.getElementById("staff-role").textContent = staff.role;

    applyRolePermissions(staff.role);
    renderPage("dashboard");
}

function applyRolePermissions(role) {
    const permissions = {
        ADMIN: ["dashboard", "customers", "accounts", "cards"],
        MANAGER: ["dashboard", "customers", "accounts", "cards"],
        TELLER: ["dashboard", "accounts", "cards"]
    };

    const allowed = permissions[role] || ["dashboard"];

    document.querySelectorAll(".nav-link").forEach(button => {
        const permitted = allowed.includes(button.dataset.page);
        button.classList.toggle("hidden", !permitted);
    });
}

loginForm.addEventListener("submit", async event => {
    event.preventDefault();

    loginError.textContent = "";
    loginButton.disabled = true;
    loginButton.textContent = "Signing in...";

    const staffNumber = document.getElementById("staff-number").value.trim();
    const password = document.getElementById("staff-password").value;

    try {
        const staff = await api("/staff-auth/login", {
            method: "POST",
            body: JSON.stringify({ staffNumber, password })
        });

        document.getElementById("staff-password").value = "";
        showApplication(staff);
    } catch (error) {
        showLogin(error.message);
    }
});

document.getElementById("logout-button").addEventListener("click", async () => {
    const button = document.getElementById("logout-button");
    button.disabled = true;

    try {
        await api("/staff-auth/logout", {
            method: "POST"
        });

        showLogin("You have signed out successfully.");
    } catch (error) {
        showLogin(error.message);
    } finally {
        button.disabled = false;
    }
});

document.getElementById("sidebar-nav").addEventListener("click", event => {
    const button = event.target.closest(".nav-link");

    if (!button || button.classList.contains("hidden")) {
        return;
    }

    renderPage(button.dataset.page);
});

function renderPage(page) {
    const pages = {
        dashboard: {
            title: "Overview",
            heading: "Banking operations",
            description: "Operational activity and account oversight.",
            content: `
                <div class="panel dashboard-welcome">
                    <h3>Welcome, ${escapeHTML(currentStaff.fullName)}</h3>
                    <p class="muted">Staff workspace · ${escapeHTML(currentStaff.role)}</p>
                </div>

                <div class="panel">
                    <div class="page-heading">
                        <h3>Operational summary</h3>
                        <button type="button" id="refresh-dashboard">Refresh</button>
                    </div>
                    <p id="dashboard-updated" class="muted">Loading operational data...</p>
                    <div id="dashboard-summary" class="dashboard-summary"></div>
                </div>

                <div class="dashboard-panels">
                    <div class="panel">
                        <h3>Account exceptions</h3>
                        <div id="dashboard-account-exceptions" class="empty-state">Loading...</div>
                    </div>
                    <div class="panel">
                        <h3>ATM card exceptions</h3>
                        <div id="dashboard-card-exceptions" class="empty-state">Loading...</div>
                    </div>
                </div>

                <div class="panel">
                    <div class="page-heading">
                        <h3>Recent banking activity</h3>
                    </div>
                    <div id="dashboard-activity" class="empty-state">Loading recent activity...</div>
                </div>

                <p id="dashboard-message" role="alert"></p>
            `
        },
        customers: {
            title: "Customers",
            heading: "Customer management",
            description: "Register customers and manage customer records.",
            content: `
                <div class="panel">
                    <h3>Register a customer</h3>
                    <form id="customer-form">
                        <label for="customer-number">Customer number</label>
                        <input id="customer-number" type="text" required>

                        <label for="customer-name">Full name</label>
                        <input id="customer-name" type="text" required>

                        <label for="customer-email">Email address</label>
                        <input id="customer-email" type="email" required>

                        <label for="customer-phone">Phone number</label>
                        <input id="customer-phone" type="tel"
                            pattern="\\+?[0-9]{9,15}" required>

                        <p id="customer-message" role="alert"></p>
                        <button type="submit">Register customer</button>
                    </form>
                </div>

                <div class="panel">
                    <div class="page-heading">
                        <h3>Customer records</h3>
                        <button id="refresh-customers" type="button">
                            Refresh records
                        </button>
                    </div>
                    <div id="customer-results" class="empty-state">
                        Loading customer records...
                    </div>
                </div>
            `
        },

        accounts: {
            title: "Accounts",
            heading: "Account management",
            description: "Look up accounts, review balances and manage banking operations.",
            content: `
                <div class="panel">
                    <h3>Find an account</h3>
                    <form id="account-search-form">
                        <label for="account-number">Account number</label>
                        <input id="account-number" type="text"
                            autocomplete="off" required
                            placeholder="Enter account number">
                        <button type="submit">Search account</button>
                    </form>
                    <p id="account-message" role="alert"></p>
                </div>

                <div id="account-results">
                    <div class="empty-state">
                        Search for an account to view its details.
                    </div>
                </div>
            `
        },

        cards: {
            title: "ATM Cards",
            heading: "ATM card administration",
            description: "Issue ATM cards to eligible bank accounts.",
            content: `
                <div class="panel">
                    <h3>Issue a new ATM card</h3>
                    <p class="muted">
                        An active bank account and a new four-digit ATM PIN
                        are required.
                    </p>

                    <form id="card-issue-form">
                        <label for="card-account">Account number</label>
                        <input id="card-account" type="text"
                            autocomplete="off" required>

                        <label for="card-pin">New ATM PIN</label>
                        <input id="card-pin" type="password"
                            inputmode="numeric" maxlength="4"
                            pattern="[0-9]{4}" required>

                        <p id="card-message" class="error"
                            role="alert"></p>

                        <button type="submit">Issue ATM card</button>
                    </form>
                </div>

                <div class="panel">
                    <h3>Find an ATM card</h3>
                    <form id="card-search-form">
                        <label for="card-search-number">Card number</label>
                        <input id="card-search-number" type="text"
                            inputmode="numeric" maxlength="16"
                            autocomplete="off" required
                            placeholder="Enter 16-digit card number">
                        <button type="submit">Search card</button>
                    </form>
                    <p id="card-search-message" role="alert"></p>
                    <div id="card-search-results">
                        <div class="empty-state">
                            Search for a card to view its details.
                        </div>
                    </div>
                </div>
            `
        }
    };

    const selected = pages[page] || pages.dashboard;

    document.getElementById("page-title").textContent = selected.title;

    document.querySelectorAll(".nav-link").forEach(button => {
        button.classList.toggle(
            "active",
            button.dataset.page === page
        );
    });

    pageContent.innerHTML = `
        <div class="page-heading">
            <h3>${selected.heading}</h3>
            <p class="muted">${selected.description}</p>
        </div>
        ${selected.content}
    `;

    if (page === "cards") {
        setupCardIssueForm();
        setupCardLookup();
    }

        if (page === "customers") {
            setupCustomerManagement();
        }

        if (page === "accounts") {
            setupAccountManagement();
        }

        if (page === "dashboard") {
            setupDashboard();
        }
}

function setupCardLookup() {
    const form = document.getElementById("card-search-form");
    const results = document.getElementById("card-search-results");
    const message = document.getElementById("card-search-message");

    form.addEventListener("submit", async event => {
        event.preventDefault();

        const cardNumber =
            document.getElementById("card-search-number").value.trim();
        const button = form.querySelector("button[type='submit']");

        message.textContent = "";
        results.innerHTML = "";
        button.disabled = true;
        button.textContent = "Searching...";

        try {
            const card = await api(
                `/atm/cards/${encodeURIComponent(cardNumber)}`
            );

            const canBlock =
                ["ADMIN", "MANAGER"].includes(currentStaff?.role);

            results.innerHTML = `
                <div class="panel">
                    <h3>Card details</h3>
                    <p><strong>Card number:</strong> ${escapeHTML(card.cardNumber)}</p>
                    <p><strong>Linked account:</strong> ${escapeHTML(card.accountNumber)}</p>
                    <p><strong>Status:</strong> ${escapeHTML(card.status)}</p>
                    <p><strong>Expiry date:</strong> ${escapeHTML(card.expiryDate)}</p>
                    <p><strong>Issued:</strong> ${escapeHTML(card.createdAt)}</p>
                    ${canBlock && card.status === "ACTIVE" ? `
                        <button type="button" id="block-card-button"
                            class="danger-button">
                            Block card
                        </button>
                    ` : ""}
                </div>
            `;

            const blockButton =
                document.getElementById("block-card-button");

            if (blockButton) {
                blockButton.addEventListener("click", async () => {
                    if (!confirm("Are you sure you want to block this ATM card?")) {
                        return;
                    }

                    blockButton.disabled = true;
                    blockButton.textContent = "Blocking...";

                    try {
                        const updated = await api(
                            `/atm/cards/${encodeURIComponent(cardNumber)}/block`,
                            { method: "PATCH" }
                        );

                        message.className = "notice success";
                        message.textContent = "ATM card blocked successfully.";

                        results.querySelector("p:nth-of-type(3)").innerHTML =
                            `<strong>Status:</strong> ${escapeHTML(updated.status)}`;

                        blockButton.remove();
                    } catch (error) {
                        message.className = "notice error";
                        message.textContent = error.message;
                        blockButton.disabled = false;
                        blockButton.textContent = "Block card";
                    }
                });
            }
        } catch (error) {
            message.className = "notice error";
            message.textContent = error.message;
            results.innerHTML = "";
        } finally {
            button.disabled = false;
            button.textContent = "Search card";
        }
    });
}

function setupCardIssueForm() {
    const form = document.getElementById("card-issue-form");

    form.addEventListener("submit", async event => {
        event.preventDefault();

        const message = document.getElementById("card-message");
        const submitButton = form.querySelector("button[type='submit']");

        const accountNumber =
            document.getElementById("card-account").value.trim();

        const pin = document.getElementById("card-pin").value;

        message.textContent = "";
        message.className = "error";
        submitButton.disabled = true;
        submitButton.textContent = "Issuing...";

        try {
            const result = await api("/atm/cards", {
                method: "POST",
                body: JSON.stringify({ accountNumber, pin })
            });

            document.getElementById("card-pin").value = "";

            message.className = "notice success";
            message.textContent =
                `Card issued successfully. Card number: ${result.cardNumber}. ` +
                `Account: ${result.accountNumber}. Expiry: ${result.expiryDate}. ` +
                "Record the card number securely; it is returned only once.";
        } catch (error) {
            message.textContent = error.message;
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = "Issue ATM card";
        }
    });
}



async function setupAccountManagement() {
    const form = document.getElementById("account-search-form");
    const results = document.getElementById("account-results");
    const message = document.getElementById("account-message");
    const searchButton = form.querySelector("button[type='submit']");

    results.addEventListener("submit", async event => {
        const form = event.target;

        if (!["deposit-form", "withdraw-form"].includes(form.id)) {
            return;
        }

        event.preventDefault();

        const operation = form.id === "deposit-form"
            ? "deposit"
            : "withdraw";

        const amountInput = form.querySelector("input[type='number']");
        const amount = Number(amountInput.value);
        const operationMessage =
            document.getElementById("cash-operation-message");
        const submitButton = form.querySelector("button[type='submit']");
        const accountNumber =
            document.getElementById("account-number").value.trim();

        if (!Number.isFinite(amount) || amount <= 0) {
            operationMessage.className = "notice error";
            operationMessage.textContent = "Enter a valid amount greater than zero.";
            return;
        }

        submitButton.disabled = true;
        submitButton.textContent = "Processing...";
        operationMessage.textContent = "";

        try {
            const result = await api(
                `/accounts/${encodeURIComponent(accountNumber)}/${operation}`,
                {
                    method: "POST",
                    body: JSON.stringify({ amount })
                }
            );

            operationMessage.className = "notice success";
            operationMessage.textContent =
                `${operation === "deposit" ? "Deposit" : "Withdrawal"} processed successfully.`;

            form.reset();
            form.requestSubmit === undefined
                ? null
                : await refreshAccount();
        } catch (error) {
            operationMessage.className = "notice error";
            operationMessage.textContent = error.message;
        } finally {
            submitButton.disabled = false;
            submitButton.textContent =
                operation === "deposit" ? "Process deposit" : "Process withdrawal";
        }
    });

    async function refreshAccount() {
        form.requestSubmit;
        document.getElementById("account-search-form").requestSubmit();
    }

    results.addEventListener("click", async event => {
        const button = event.target.closest("[data-account-action]");

        if (!button || !["ADMIN", "MANAGER"].includes(currentStaff?.role)) {
            return;
        }

        const action = button.dataset.accountAction;
        const accountNumber =
            document.getElementById("account-number").value.trim();

        button.disabled = true;
        button.textContent = "Updating...";

        try {
            await api(`/accounts/${encodeURIComponent(accountNumber)}/${action}`, {
                method: "PATCH"
            });

            message.className = "notice success";
            message.textContent = `Account ${action} operation completed.`;

            form.requestSubmit();
        } catch (error) {
            message.className = "notice error";
            message.textContent = error.message;
            button.disabled = false;
            button.textContent = action.charAt(0).toUpperCase() + action.slice(1);
        }
    });

    form.addEventListener("submit", async event => {
        event.preventDefault();

        const accountNumber =
            document.getElementById("account-number").value.trim();

        message.textContent = "";
        message.className = "";
        results.className = "empty-state";
        results.textContent = "Searching for account...";
        searchButton.disabled = true;
        searchButton.textContent = "Searching...";

        try {
            const encodedNumber = encodeURIComponent(accountNumber);

            const account = await api(`/accounts/${encodedNumber}`);

            let balance = null;
            let balanceError = "";

            try {
                balance = await api(`/accounts/${encodedNumber}/balance`);
            } catch (error) {
                balanceError = error.message;
            }

            let transactions = [];
            let transactionError = "";

            try {
                transactions = await api(
                    `/accounts/${encodedNumber}/transactions`
                );
            } catch (error) {
                transactionError = error.message;
            }

            const fields = [
                ["Account number", account.accountNumber],
                ["Account holder", account.accountHolderName],
                ["Customer number", account.customerNumber],
                ["Account type", account.accountType],
                ["Status", account.status],
                ["Created", account.createdAt]
            ];

            const accountDetails = fields
                .filter(([, value]) => value !== undefined && value !== null)
                .map(([label, value]) => `
                    <div class="stat-card">
                        <p>${escapeHTML(label)}</p>
                        <h3>${escapeHTML(value)}</h3>
                    </div>
                `).join("");

            let balanceDetails = "";

            if (balance && typeof balance === "object") {
                const balanceFields = Object.entries(balance)
                    .filter(([key, value]) =>
                        /balance|amount|currency/i.test(key) &&
                        (typeof value === "string" || typeof value === "number")
                    );

                balanceDetails = balanceFields.map(([key, value]) => `
                    <div class="stat-card">
                        <p>${escapeHTML(key.replace(/([A-Z])/g, " $1"))}</p>
                        <h3>${escapeHTML(value)}</h3>
                    </div>
                `).join("");
            }

            const statusActions = ["ADMIN", "MANAGER"].includes(currentStaff?.role)
                ? `
                    <div class="panel">
                        <h3>Account status management</h3>
                        <div class="toolbar">
                            <button type="button" data-account-action="freeze">Freeze</button>
                            <button type="button" data-account-action="suspend">Suspend</button>
                            <button type="button" data-account-action="activate">Activate</button>
                        </div>
                    </div>
                `
                : "";

            results.className = "";
            results.innerHTML = `
                <div class="panel">
                    <h3>Account details</h3>
                    <div class="stats-grid">
                        ${accountDetails || '<p class="muted">No account details available.</p>'}
                    </div>
                </div>

                <div class="panel">
                    <h3>Account balance</h3>
                    <div class="stats-grid">
                        ${balanceDetails || `
                            <p class="muted">
                                ${escapeHTML(balanceError || "Balance information is unavailable.")}
                            </p>
                        `}
                    </div>
                </div>

                <div class="panel">
                    <h3>Cash operations</h3>
                    <div class="form-grid">
                        <form id="deposit-form">
                            <h3>Deposit</h3>
                            <label for="deposit-amount">Amount</label>
                            <input id="deposit-amount" type="number"
                                min="0.01" step="0.01" required>
                            <button type="submit">Process deposit</button>
                        </form>

                        <form id="withdraw-form">
                            <h3>Withdrawal</h3>
                            <label for="withdraw-amount">Amount</label>
                            <input id="withdraw-amount" type="number"
                                min="0.01" step="0.01" required>
                            <button type="submit">Process withdrawal</button>
                        </form>
                    </div>
                    <p id="cash-operation-message" role="alert"></p>
                </div>

                ${statusActions}

                <div class="panel">
                    <h3>Recent transactions</h3>
                    ${Array.isArray(transactions) && transactions.length > 0
                        ? `
                            <div class="table-wrap">
                                <table>
                                    <thead>
                                        <tr>
                                            <th>Reference</th>
                                            <th>Type</th>
                                            <th>Amount</th>
                                            <th>Status</th>
                                            <th>Date</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        ${transactions.map(transaction => `
                                            <tr>
                                                <td>${escapeHTML(transaction.reference || transaction.transactionId || "—")}</td>
                                                <td>${escapeHTML(transaction.type || transaction.transactionType || "—")}</td>
                                                <td>${escapeHTML(transaction.amount ?? "—")}</td>
                                                <td>${escapeHTML(transaction.status || "—")}</td>
                                                <td>${escapeHTML(transaction.createdAt || transaction.timestamp || "—")}</td>
                                            </tr>
                                        `).join("")}
                                    </tbody>
                                </table>
                            </div>
                        `
                        : `<p class="muted">${escapeHTML(transactionError || "No recent transactions found.")}</p>`
                    }
                </div>
            `;
        } catch (error) {
            results.textContent = error.message;
            results.className = "empty-state error";
        } finally {
            searchButton.disabled = false;
            searchButton.textContent = "Search account";
        }
    });
}

async function setupCustomerManagement() {
    const form = document.getElementById("customer-form");
    const results = document.getElementById("customer-results");
    const message = document.getElementById("customer-message");
    const refreshButton = document.getElementById("refresh-customers");

    async function loadCustomers() {
        results.textContent = "Loading customer records...";
        results.className = "empty-state";
        refreshButton.disabled = true;

        try {
            const customers = await api("/customers");

            if (!Array.isArray(customers) || customers.length === 0) {
                results.textContent = "No customer records found.";
                return;
            }

            results.className = "";
            results.innerHTML = `
                <div class="table-wrap">
                    <table>
                        <thead>
                            <tr>
                                <th>Customer number</th>
                                <th>Full name</th>
                                <th>Email</th>
                                <th>Phone</th>
                                <th>Registered</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${customers.map(customer => `
                                <tr>
                                    <td>${escapeHTML(customer.customerNumber)}</td>
                                    <td>${escapeHTML(customer.fullName)}</td>
                                    <td>${escapeHTML(customer.email)}</td>
                                    <td>${escapeHTML(customer.phoneNumber)}</td>
                                    <td>${escapeHTML(customer.createdAt
                                        ? new Date(customer.createdAt).toLocaleDateString()
                                        : "—")}</td>
                                </tr>
                            `).join("")}
                        </tbody>
                    </table>
                </div>
            `;
        } catch (error) {
            results.textContent = error.message;
            results.className = "empty-state error";
        } finally {
            refreshButton.disabled = false;
        }
    }

    refreshButton.addEventListener("click", loadCustomers);

    form.addEventListener("submit", async event => {
        event.preventDefault();

        const submitButton = form.querySelector("button[type='submit']");
        const customer = {
            customerNumber: document.getElementById("customer-number").value.trim(),
            fullName: document.getElementById("customer-name").value.trim(),
            email: document.getElementById("customer-email").value.trim(),
            phoneNumber: document.getElementById("customer-phone").value.trim()
        };

        message.textContent = "";
        message.className = "";
        submitButton.disabled = true;
        submitButton.textContent = "Registering...";

        try {
            const created = await api("/customers", {
                method: "POST",
                body: JSON.stringify(customer)
            });

            message.className = "notice success";
            message.textContent =
                `Customer ${created.customerNumber} registered successfully.`;

            form.reset();
            await loadCustomers();
        } catch (error) {
            message.className = "error";
            message.textContent = error.message;
        } finally {
            submitButton.disabled = false;
            submitButton.textContent = "Register customer";
        }
    });

    await loadCustomers();
}

function escapeHTML(value) {
    return String(value ?? "").replace(/[&<>"']/g, character => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[character]);
}


async function setupDashboard() {
    const summary = document.getElementById("dashboard-summary");
    const accountExceptions = document.getElementById("dashboard-account-exceptions");
    const cardExceptions = document.getElementById("dashboard-card-exceptions");
    const activity = document.getElementById("dashboard-activity");
    const updated = document.getElementById("dashboard-updated");
    const message = document.getElementById("dashboard-message");
    const refreshButton = document.getElementById("refresh-dashboard");

    const money = value => new Intl.NumberFormat("en-ZA", {
        style: "currency",
        currency: "ZAR"
    }).format(Number(value ?? 0));

    const dateTime = value => {
        if (!value) return "—";
        const parsed = new Date(value);
        return Number.isNaN(parsed.getTime())
            ? escapeHTML(value)
            : escapeHTML(parsed.toLocaleString("en-ZA"));
    };

    const count = value => Number(value ?? 0).toLocaleString("en-ZA");

    async function loadDashboard() {
        refreshButton.disabled = true;
        refreshButton.textContent = "Refreshing...";
        message.textContent = "";
        summary.innerHTML = '<p class="muted">Loading operational data...</p>';
        accountExceptions.innerHTML = '<p class="muted">Loading...</p>';
        cardExceptions.innerHTML = '<p class="muted">Loading...</p>';
        activity.innerHTML = '<p class="muted">Loading recent activity...</p>';

        try {
            const data = await api("/staff/dashboard");

            const summaryItems = [
                ...(currentStaff?.role !== "TELLER"
                    ? [{ label: "Customers", value: data.totalCustomers }]
                    : []),
                { label: "Total accounts", value: data.totalAccounts },
                { label: "Active accounts", value: data.activeAccounts },
                { label: "Total ATM cards", value: data.totalCards },
                { label: "Active cards", value: data.activeCards }
            ];

            summary.innerHTML = `
                <div class="dashboard-summary-list">
                    ${summaryItems.map(item => `
                        <div class="dashboard-summary-item">
                            <span>${escapeHTML(item.label)}</span>
                            <strong>${count(item.value)}</strong>
                        </div>
                    `).join("")}
                </div>
            `;

            const accountItems = [
                { label: "Frozen accounts", value: data.frozenAccounts },
                { label: "Suspended accounts", value: data.suspendedAccounts }
            ];

            accountExceptions.innerHTML = `
                <div class="dashboard-exception-list">
                    ${accountItems.map(item => `
                        <div class="dashboard-exception-item">
                            <span>${escapeHTML(item.label)}</span>
                            <strong>${count(item.value)}</strong>
                        </div>
                    `).join("")}
                </div>
            `;

            const cardItems = [
                { label: "Blocked cards", value: data.blockedCards },
                { label: "Expired cards", value: data.expiredCards },
                { label: "Temporarily locked cards", value: data.temporarilyLockedCards }
            ];

            cardExceptions.innerHTML = `
                <div class="dashboard-exception-list">
                    ${cardItems.map(item => `
                        <div class="dashboard-exception-item">
                            <span>${escapeHTML(item.label)}</span>
                            <strong>${count(item.value)}</strong>
                        </div>
                    `).join("")}
                </div>
            `;

            const entries = Array.isArray(data.recentActivity)
                ? data.recentActivity
                : [];

            if (entries.length === 0) {
                activity.innerHTML = '<div class="empty-state">No banking transactions have been recorded yet.</div>';
            } else {
                const labels = {
                    DEPOSIT: "Deposit",
                    WITHDRAWAL: "Withdrawal",
                    TRANSFER_SENT: "Transfer sent",
                    TRANSFER_RECEIVED: "Transfer received"
                };

                activity.innerHTML = `
                    <div class="table-wrap">
                        <table>
                            <thead>
                                <tr>
                                    <th>Transaction</th>
                                    <th>Account</th>
                                    <th>Account holder</th>
                                    <th>Amount</th>
                                    <th>Balance after</th>
                                    <th>Date and time</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${entries.map(entry => `
                                    <tr>
                                        <td>${escapeHTML(labels[entry.type] || entry.type || "Transaction")}</td>
                                        <td>${escapeHTML(entry.accountNumber)}</td>
                                        <td>${escapeHTML(entry.accountHolderName)}</td>
                                        <td>${money(entry.amount)}</td>
                                        <td>${money(entry.balanceAfter)}</td>
                                        <td>${dateTime(entry.createdAt)}</td>
                                    </tr>
                                `).join("")}
                            </tbody>
                        </table>
                    </div>
                `;
            }

            updated.textContent = `Last updated: ${new Date().toLocaleString("en-ZA")}`;
        } catch (error) {
            summary.innerHTML = '<p class="muted">Operational data could not be loaded.</p>';
            accountExceptions.innerHTML = '<p class="muted">Unavailable.</p>';
            cardExceptions.innerHTML = '<p class="muted">Unavailable.</p>';
            activity.innerHTML = '<div class="empty-state">Recent activity is unavailable.</div>';
            message.className = "notice error";
            message.textContent = error.message;
            updated.textContent = "Dashboard refresh failed.";
        } finally {
            refreshButton.disabled = false;
            refreshButton.textContent = "Refresh";
        }
    }

    refreshButton.addEventListener("click", loadDashboard);
    await loadDashboard();
}

async function restoreSession() {
    try {
        const staff = await api("/staff-auth/me");
        showApplication(staff);
    } catch {
        showLogin();
    }
}

restoreSession();
