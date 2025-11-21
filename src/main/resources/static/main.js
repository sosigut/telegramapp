const tg = window.Telegram.WebApp;
const backendUrl = "https://telegramapp-production.up.railway.app";
const now = new Date();
const formattedDate = now.toLocaleString();

tg.ready();
tg.expand();

// BASE URL
const API = "https://telegramapp-production.up.railway.app/api/transaction";

document.getElementById("addBtn").onclick = addTransaction;

loadTransactions();

async function addTransaction() {
    const type = document.getElementById("type").value;
    const category = document.getElementById("category").value;
    const amount = parseFloat(document.getElementById("amount").value);
    const description = document.getElementById("description").value;
    const date = new Date().toISOString(); // ISO формат

    if (!amount) {
        tg.showAlert("Enter amount!");
        return;
    }

    const body = { type, category, amount, description, date };

    const res = await fetch(API, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    });

    if (!res.ok) {
        tg.showAlert("Failed to add transaction: " + res.statusText);
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
    const res = await fetch(API);
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
    container.innerHTML = ""; // очищаем список

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
        if (t.type === "expense") {
            stats[t.category] = (stats[t.category] || 0) + Number(t.amount);
        }
    });
    return stats;
}

document.getElementById("search-btn").onclick = async () => {
    let category = document.getElementById("filter-category").value;
    const start = document.getElementById("filter-start").value;
    const end = document.getElementById("filter-end").value;

// Если выбран "All", category = null
    if (category === "") category = null;

    let url = backendUrl + "/api/transaction/search?";

    if (category) url += "category=" + encodeURIComponent(category) + "&";
    if (start) url += "start=" + start + "&";
    if (end) url += "end=" + end;

    try {
        const res = await fetch(url);
        const data = await res.json();

        // Гарантируем, что data массив
        const transactions = Array.isArray(data) ? data : [];

        if (transactions.length === 0) {
            tg.showAlert("No transactions found for the given filter");
        }

        renderTransactions(transactions);
        drawExpenseChart(buildExpenseStats(transactions));
    } catch (e) {
        tg.showAlert("Search error: " + e);
    }

}


async function deleteTransaction(id) {
    await fetch(`${API}/${id}`, { method: "DELETE" });
    await loadTransactions();
}

let expenseChart = null;

