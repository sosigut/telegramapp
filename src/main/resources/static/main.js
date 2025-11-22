const tg = window.Telegram.WebApp;

tg.ready();
tg.expand();

// BASE URL
const API = "https://telegramapp-production.up.railway.app/api/transaction";

// Получаем ID пользователя из Telegram Web App
const userId = tg.initDataUnsafe?.user?.id || tg.initDataUnsafe?.user?.id || 'default_user_id';

console.log("User ID:", userId);

// Функция для получения заголовков с userId
function getHeaders() {
    return {
        'Content-Type': 'application/json',
        'X-User-Id': userId.toString()
    };
}

document.getElementById("addBtn").onclick = addTransaction;

loadTransactions();

async function addTransaction() {
    const type = document.getElementById("type").value;
    const category = document.getElementById("category").value;
    const amount = parseFloat(document.getElementById("amount").value);
    const description = document.getElementById("description").value;

    if (!amount) {
        tg.showAlert("Enter amount!");
        return;
    }

    const body = { type, category, amount, description};

    const res = await fetch(API, {
        method: "POST",
        headers: getHeaders(),
        body: JSON.stringify(body)
    });

    if (!res.ok) {
        const errorText = await res.text();
        tg.showAlert("Failed to add transaction: " + errorText);
        return;
    }

    document.getElementById("amount").value = "";
    document.getElementById("description").value = "";

    await loadTransactions();
}

function drawExpenseChart(categoryTotals) {
    const ctx = document.getElementById('expenseChart').getContext('2d');

    if (expenseChart !== null) {
        expenseChart.destroy();
    }

    expenseChart = new Chart(ctx, {
        type: 'pie',
        data: {
            labels: Object.keys(categoryTotals),
            datasets: [{
                data: Object.values(categoryTotals),
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false
        }
    });
}

async function loadTransactions() {
    const res = await fetch(API, {
        headers: getHeaders()
    });

    if (!res.ok) {
        console.error("Failed to load transactions");
        return;
    }

    const data = await res.json();

    // Calculate summary
    let income = 0;
    let expense = 0;

    data.forEach(t => {
        if (t.type === "Income") income += t.amount;
        else expense += t.amount;
    });

    const balance = income - expense;

    // Update UI
    document.getElementById("sum-balance").innerText = balance.toFixed(2);
    document.getElementById("sum-income").innerText = income.toFixed(2);
    document.getElementById("sum-expense").innerText = expense.toFixed(2);

    // Render transactions
    const container = document.getElementById("transactions");
    container.innerHTML = "";

    data.forEach(t => {
        const card = document.createElement("div");
        card.className = "transaction-card";

        card.innerHTML = `
        <div class="transaction-title">${t.type.toUpperCase()} | ${t.category}</div>
        <div>Amount: <b>${t.amount}</b></div>
        <div class="transaction-desc">${t.description ?? ""}</div>
        <div class="transaction-date">${new Date(t.date).toLocaleString()}</div>
        <button class="btn-danger" onclick="deleteTransaction(${t.id})">Delete</button>
    `;
        container.appendChild(card);
    });

    let categoryTotals = {};

    data.forEach(t => {
        if (t.type === "Expense") {
            categoryTotals[t.category] = (categoryTotals[t.category] || 0) + t.amount;
        }
    });

    drawExpenseChart(categoryTotals);
}

function renderTransactions(transactions) {
    const container = document.getElementById("transactions");
    container.innerHTML = "";

    if (transactions.length === 0) {
        container.innerHTML = `
            <div class="card" style="text-align: center; color: var(--text-light);">
                No transactions found
            </div>
        `;
        return;
    }

    transactions.forEach(t => {
        const div = document.createElement("div");
        div.className = "transaction-card";

        div.innerHTML = `
        <div class="transaction-title">${t.type.toUpperCase()} | ${t.category}</div>
        <div>Amount: <b>${t.amount}</b></div>
        <div class="transaction-desc">${t.description ?? ""}</div>
        <div class="transaction-date">${new Date(t.date).toLocaleString()}</div>
        <button class="btn-danger" onclick="deleteTransaction(${t.id})">Delete</button>
    `;

        container.appendChild(div);
    });
}

function buildExpenseStats(transactions) {
    const stats = {};
    transactions.forEach(t => {
        if (t.type === "Expense") {
            stats[t.category] = (stats[t.category] || 0) + Number(t.amount);
        }
    });
    return stats;
}

document.getElementById("search-btn").onclick = async () => {
    let category = document.getElementById("filter-category").value;
    const start = document.getElementById("filter-start").value;
    const end = document.getElementById("filter-end").value;

    console.log("Search params:", { category, start, end });

    const params = new URLSearchParams();

    if (category && category !== "Все") {
        params.append('category', category);
    }
    if (start) {
        params.append('start', start);
    }
    if (end) {
        params.append('end', end);
    }

    const url = `${API}/search?${params.toString()}`;
    console.log("Final URL:", url);

    try {
        const res = await fetch(url, {
            headers: getHeaders()
        });

        if (!res.ok) {
            throw new Error(`HTTP error! status: ${res.status}`);
        }

        const data = await res.json();
        console.log("Found transactions:", data);

        const transactions = Array.isArray(data) ? data : [];

        if (transactions.length === 0) {
            tg.showAlert("Транзакции по заданным фильтрам не найдены");
        } else {
            tg.showAlert(`Найдено ${transactions.length} транзакций`);
        }

        renderTransactions(transactions);
        drawExpenseChart(buildExpenseStats(transactions));
    } catch (e) {
        console.error("Search error:", e);
        tg.showAlert("Ошибка поиска: " + e.message);
    }
}

async function deleteTransaction(id) {
    await fetch(`${API}/${id}`, {
        method: "DELETE",
        headers: getHeaders()
    });
    await loadTransactions();
}

let expenseChart = null;