const tg = window.Telegram.WebApp;

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

    if (!amount) {
        tg.showAlert("Enter amount!");
        return;
    }

    const body = {
        id: Date.now(),
        type,
        category,
        amount,
        description
    };

    await fetch(API, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    });

    document.getElementById("amount").value = "";
    document.getElementById("description").value = "";

    loadTransactions();
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
            <button class="btn-danger" onclick="deleteTransaction(${t.id})">Delete</button>
        `;

        container.appendChild(card);
    });
}

async function deleteTransaction(id) {
    await fetch(`${API}/${id}`, { method: "DELETE" });
    loadTransactions();
}
