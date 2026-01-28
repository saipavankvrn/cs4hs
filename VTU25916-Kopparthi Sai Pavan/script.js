const form = document.getElementById("contactForm");
const error = document.getElementById("error");

form.addEventListener("submit", async (e) => {
  e.preventDefault();

  const name = document.getElementById("name").value.trim();
  const email = document.getElementById("email").value.trim();
  const message = document.getElementById("message").value.trim();

  if (name.length < 3) {
    showError("Name must be at least 3 characters");
    return;
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email)) {
    showError("Invalid email format");
    return;
  }

  if (message.length < 10) {
    showError("Message must be at least 10 characters");
    return;
  }

  // SEND TO BACKEND
  try {
    const res = await fetch("http://127.0.0.1:5000/contact", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name, email, message })
    });

    const data = await res.json();
    showSuccess(data.message);
    form.reset();
  } catch {
    showError("Server error. Try again later.");
  }
});

function showError(msg) {
  error.style.color = "red";
  error.textContent = msg;
}

function showSuccess(msg) {
  error.style.color = "green";
  error.textContent = msg;
}
