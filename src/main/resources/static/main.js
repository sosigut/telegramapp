const tg = window.Telegram.WebApp;

// Расширяем на весь экран
tg.expand();

// Выводим данные о пользователе
console.log("User:", tg.initDataUnsafe);

// Обработчик на кнопку
document.getElementById("add-expense-btn").onclick = () => {
    tg.showAlert("Нажата кнопка 'Add Expense'");
};
