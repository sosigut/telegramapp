const tg = window.Telegram.WebApp;

tg.ready();
tg.expand();

// BASE URL
const API = "https://telegramapp-production.up.railway.app/api/transaction";

// Получаем ID пользователя из Telegram Web App
const userId = tg.initDataUnsafe?.user?.id || 'default_user_id';

console.log("User ID:", userId);

// Функция для получения заголовков с userId
function getHeaders() {
    return {
        'Content-Type': 'application/json',
        'X-User-Id': userId.toString()
    };
}

// Инициализация при загрузке
document.addEventListener('DOMContentLoaded', function() {
    initTabs();
    loadTransactions();

    // Назначаем обработчики
    document.getElementById("addBtn").onclick = addTransaction;
    document.getElementById("search-btn").onclick = searchTransactions;
    document.getElementById("reset-btn").onclick = resetFilters;
    document.getElementById("ai-advice-btn").onclick = getAIAdvice; // Добавлен обработчик для AI
});

// Инициализация вкладок
function initTabs() {
    const tabButtons = document.querySelectorAll('.tab-button');
    const tabContents = document.querySelectorAll('.tab-content');

    tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            const tabId = button.getAttribute('data-tab');

            // Убираем активный класс у всех кнопок и контента
            tabButtons.forEach(btn => btn.classList.remove('active'));
            tabContents.forEach(content => content.classList.remove('active'));

            // Добавляем активный класс текущей кнопке и контенту
            button.classList.add('active');
            document.getElementById(tabId).classList.add('active');

            // Если перешли на вкладку аналитики - обновляем график
            if (tabId === 'analytics') {
                updateAnalytics();
            }
        });
    });
}

// Функция для переключения на конкретную вкладку
function switchToTab(tabName) {
    const tabButton = document.querySelector(`[data-tab="${tabName}"]`);
    if (tabButton) {
        tabButton.click();
    }
}

async function addTransaction() {
    const type = document.getElementById("type").value;
    const category = document.getElementById("category").value;
    const amount = parseFloat(document.getElementById("amount").value);
    const description = document.getElementById("description").value;

    if (!amount) {
        tg.showAlert("Введите сумму!");
        return;
    }

    const body = { type, category, amount, description};

    try {
        const res = await fetch(API, {
            method: "POST",
            headers: getHeaders(),
            body: JSON.stringify(body)
        });

        if (!res.ok) {
            const errorText = await res.text();
            tg.showAlert("Ошибка добавления транзакции: " + errorText);
            return;
        }

        document.getElementById("amount").value = "";
        document.getElementById("description").value = "";

        await loadTransactions();
        tg.showAlert("Транзакция успешно добавлена!");

        // Переключаемся на вкладку транзакций после добавления
        switchToTab('transactions');

    } catch (error) {
        console.error("Error adding transaction:", error);
        tg.showAlert("Ошибка при добавлении транзакции");
    }
}

// Функция для получения AI совета
async function getAIAdvice() {
    const button = document.getElementById("ai-advice-btn");
    const adviceText = document.getElementById("ai-advice-text");

    try {
        // Показываем загрузку
        button.disabled = true;
        button.textContent = "Анализируем...";
        adviceText.innerText = "AI анализирует ваши финансы...";

        const res = await fetch(`${API}/ai-advice`, {
            headers: getHeaders()
        });

        if (!res.ok) {
            throw new Error(`HTTP error! status: ${res.status}`);
        }

        const text = await res.text();
        adviceText.innerText = text;

    } catch (e) {
        console.error("AI advice error:", e);
        adviceText.innerText = "❌ Не удалось получить совет. Попробуйте позже.";
        tg.showAlert("Ошибка AI: " + e.message);
    } finally {
        // Восстанавливаем кнопку
        button.disabled = false;
        button.textContent = "Получить совет от AI 💡";
    }
}

function drawExpenseChart(categoryTotals) {
    const ctx = document.getElementById('expenseChart').getContext('2d');

    if (window.expenseChart !== null) {
        window.expenseChart.destroy();
    }

    // Создаем цвета для категорий
    const colors = ['#ef4444', '#f97316', '#f59e0b', '#eab308', '#84cc16', '#10b981', '#06b6d4', '#3b82f6', '#6366f1', '#8b5cf6'];

    window.expenseChart = new Chart(ctx, {
        type: 'pie',
        data: {
            labels: Object.keys(categoryTotals),
            datasets: [{
                data: Object.values(categoryTotals),
                backgroundColor: colors,
                borderColor: '#1f2937',
                borderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        color: '#f3f4f6',
                        font: {
                            size: 12
                        }
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.raw || 0;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = Math.round((value / total) * 100);
                            return `${label}: ${value.toFixed(2)} (${percentage}%)`;
                        }
                    }
                }
            }
        }
    });
}

async function loadTransactions() {
    try {
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

        // Update UI вкладки Обзор - только баланс и суммы
        document.getElementById("sum-balance").innerText = balance.toFixed(2);
        document.getElementById("sum-income").innerText = income.toFixed(2);
        document.getElementById("sum-expense").innerText = expense.toFixed(2);

        // Всегда обновляем список транзакций во вкладке "Транзакции"
        renderTransactions(data);

        // Если активна вкладка "Аналитика" - обновляем график и статистику
        if (document.getElementById('analytics').classList.contains('active')) {
            updateAnalytics(data);
        }

    } catch (error) {
        console.error("Error loading transactions:", error);
        tg.showAlert("Ошибка загрузки данных");
    }
}

// Функция для рендера транзакций (используется в loadTransactions и поиске)
function renderTransactions(transactions) {
    const container = document.getElementById("transactions-list");
    container.innerHTML = "";

    if (transactions.length === 0) {
        container.innerHTML = `
            <div class="card" style="text-align: center; color: var(--text-light);">
                Транзакции не найдены
            </div>
        `;
        return;
    }

    // Сортируем транзакции по дате (новые сверху)
    const sortedTransactions = transactions.sort((a, b) => new Date(b.date) - new Date(a.date));

    sortedTransactions.forEach(t => {
        const div = document.createElement("div");
        div.className = "transaction-card";

        // Определяем цвет в зависимости от типа транзакции
        const amountColor = t.type === "Income" ? "#10b981" : "#ef4444";
        const typeText = t.type === "Income" ? "Доход" : "Расход";
        const typeIcon = t.type === "Income" ? "📈" : "📉";

        div.innerHTML = `
            <div class="transaction-header">
                <div class="transaction-title">${typeIcon} ${typeText} | ${t.category}</div>
                <div class="transaction-amount" style="color: ${amountColor}">${t.amount.toFixed(2)}</div>
            </div>
            ${t.description ? `<div class="transaction-desc">${t.description}</div>` : ''}
            <div class="transaction-footer">
                <div class="transaction-date">${new Date(t.date).toLocaleString('ru-RU')}</div>
                <button class="btn-danger" onclick="deleteTransaction(${t.id})">Удалить</button>
            </div>
        `;
        container.appendChild(div);
    });
}

// Обновленная функция для аналитики
async function updateAnalytics(transactionsData = null) {
    try {
        let data = transactionsData;

        // Если данные не переданы, загружаем их
        if (!data) {
            const res = await fetch(API, {
                headers: getHeaders()
            });
            if (!res.ok) return;
            data = await res.json();
        }

        const stats = buildExpenseStats(data);

        // Обновляем статистику по категориям
        const statsContainer = document.getElementById('category-stats');
        statsContainer.innerHTML = '';

        // Сортируем категории по убыванию суммы
        const sortedStats = Object.entries(stats)
            .sort(([,a], [,b]) => b - a);

        if (sortedStats.length === 0) {
            statsContainer.innerHTML = `
                <div style="text-align: center; color: var(--text-light); padding: 20px;">
                    Нет данных о расходах
                </div>
            `;
            return;
        }

        sortedStats.forEach(([category, amount]) => {
            const statItem = document.createElement('div');
            statItem.className = 'stat-item';
            statItem.innerHTML = `
                <span class="stat-category">${category}</span>
                <span class="stat-amount">${amount.toFixed(2)}</span>
            `;
            statsContainer.appendChild(statItem);
        });

        // Обновляем график
        drawExpenseChart(stats);

    } catch (error) {
        console.error("Error updating analytics:", error);
    }
}

// Функция для построения статистики расходов
function buildExpenseStats(transactions) {
    const stats = {};
    transactions.forEach(t => {
        if (t.type === "Expense") {
            stats[t.category] = (stats[t.category] || 0) + Number(t.amount);
        }
    });
    return stats;
}

// Функция поиска транзакций
async function searchTransactions() {
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

        // Обновляем аналитику если на вкладке аналитики
        if (document.getElementById('analytics').classList.contains('active')) {
            updateAnalytics(transactions);
        }

    } catch (e) {
        console.error("Search error:", e);
        tg.showAlert("Ошибка поиска: " + e.message);
    }
}

// Функция сброса фильтров
async function resetFilters() {
    document.getElementById("filter-category").value = "";
    document.getElementById("filter-start").value = "";
    document.getElementById("filter-end").value = "";

    await loadTransactions();
    tg.showAlert("Фильтры сброшены");
}

// Функция удаления транзакции
async function deleteTransaction(id) {
    if (!confirm("Вы уверены, что хотите удалить эту транзакцию?")) {
        return;
    }

    try {
        const res = await fetch(`${API}/${id}`, {
            method: "DELETE",
            headers: getHeaders()
        });

        if (!res.ok) {
            throw new Error('Failed to delete transaction');
        }

        await loadTransactions();
        tg.showAlert("Транзакция удалена");
    } catch (error) {
        console.error("Error deleting transaction:", error);
        tg.showAlert("Ошибка при удалении транзакции");
    }
}

// Инициализация глобальной переменной для графика
window.expenseChart = null;