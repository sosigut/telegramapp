const tg = window.Telegram.WebApp;
tg.expand();

const backend = "https://telegramapp-production.up.railway.app";

// UI elements
const balanceEl = document.getElementById("balance-amount");
const txList = document.getElementById("transactions");

// --------------------- LOAD ALL ---------------------
async function loadTransactions() {
    const res = await fetch(backend + "/api/transaction");
    const data = await res.json();

    renderTransactions(data);
    updateBalance(data);
}

function updateBalance(list) {
    let income = 0, expense = 0;

    list.forEach(t => {
        if (t.type === "income") income += t.amount;
        else expense += t.amount;
    });

    const balance = income - expense;
    balanceEl.textContent = balance + " ₽";
}

// --------------------- RENDER ---------------------
function renderTransactions(list) {
    txList.innerHTML = "";

    if (list.length === 0) {
        txList.innerHTML = `<div class="empty">No transactions</div>`;
        return;
    }

    list.forEach(t => {
        const item = document.createElement("div");
        item.className = "transaction";

        item.innerHTML = `
            <div class="tx-left">
                <div class="tx-cat">${t.category}</div>
                <div class="tx-desc">${t.description ?? ""}</div>
                <div class="tx-date">${t.date.replace("T", " ")}</div>
            </div>
            <div class="tx-amount ${t.type}">
                ${t.type === "income" ? "+" : "-"}${t.amount} ₽
            </div>
        `;

        txList.appendChild(item);
    });
}

// --------------------- ADD ---------------------
document.getElementById("add-btn").onclick = async () => {
    const type = document.getElementById("type").value;
    const amount = Number(document.getElementById("amount").value);
    const category = document.getElementById("category").value;
    const description = document.getElementById("description").value;

    if (!amount || amount <= 0) {
        tg.showAlert("Amount must be greater than 0");
        return;
    }

    const body = {
        id: Date.now(),
        type,
        amount,
        category,
        description
    };

    await fetch(backend + "/api/transaction", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(body)
    });

    tg.showAlert("Added!");
    loadTransactions();
};

// --------------------- SEARCH ---------------------
document.getElementById("search-btn").onclick = async () => {
    const category = document.getElementById("filter-category").value;
    const start = document.getElementById("filter-start").value;
    const end = document.getElementById("filter-end").value;

    let url = backend + "/api/transaction/search?";

    if (category) url += "category=" + category + "&";
    if (start) url += "start=" + start + "&";
    if (end) url += "end=" + end;

    const res = await fetch(url);
    const data = await res.json();

    renderTransactions(data);
    updateBalance(data);
};

// Start
loadTransactions();
