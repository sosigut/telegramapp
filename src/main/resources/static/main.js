const tg = window.Telegram.WebApp;
tg.ready();
tg.expand();

const API = "https://telegramapp-production.up.railway.app/api/transaction";

// базовые категории
const categories = {
    income: ["Salary", "Gift", "Bonus", "Investments"],
    expense: ["Food", "Transport", "Shopping", "Entertainment", "Bills"]
};

// переключение категорий по типу
function updateCategories() {
    const type = document.getElementById("type").value;
    const categorySelect = document.getElementById("category");

    categorySelect.innerHTML = "";

    categories[type].forEach(c => {
        const option = document.createElement("option");
        option.value = c;
        option.innerText = c;
        categorySelect.appendChild(option);
    });
}

document.getElementById("type").onchange = updateCategories;
updateCategories(); // initial load

// добавление транзакции
document.getElementById("addTransaction").onclick = async () => {
    const t = {
        id: Date.now(),
        type: document.getElementById("type").value,
        amount: parseFloat(document.getElementById("amount").value),
        category: document.getElementById("category").value,
        description: document.getElementById("description").value
    };

    if (!t.amount || t.amount <= 0) {
        tg.showAlert("Amount must be > 0");
        return;
    }

    try {
        const res = await fetch(API, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(t)
        });

        if (!res.ok) throw new Error("Failed to add");

        tg.showAlert("Transaction added!");
        loadTransactions();
    } catch (e) {
        tg.showAlert("Error: " + e.message);
    }
};

// загрузка транзакций
async function loadTransactions() {
    try {
        const res = await fetch(API);
        const list = await res.json();

        const container = document.getElementById("transactions");
        container.innerHTML = "";

        list.forEach(t => {
            const div = document.createElement("div");
            div.className = "transaction-item";

            div.innerHTML = `
                <b>${t.type.toUpperCase()} | ${t.category}</b><br>
                Amount: ${t.amount}<br>
                ${t.description || ""}<br>
                <button class="delete-btn" onclick="deleteTransaction(${t.id})">Delete</button>
            `;

            container.appendChild(div);
        });

    } catch (e) {
        tg.showAlert("Load error: " + e.message);
    }
}

async function deleteTransaction(id) {
    try {
        await fetch(`${API}/${id}`, {method: "DELETE"});
        loadTransactions();
    } catch (e) {
        tg.showAlert("Delete error: " + e.message);
    }
}

loadTransactions();
