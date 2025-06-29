from flask import Flask, session, redirect, url_for, request, render_template
import secrets
from functools import wraps

app = Flask(__name__)
# Chiave segreta per criptare le sessioni
app.secret_key = secrets.token_hex(16)

# Decoratore per proteggere le route che richiedono il login
def login_required(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if 'email' not in session:
            return redirect(url_for('login'))
        return f(*args, **kwargs)
    return decorated_function

@app.route('/')
@login_required
def home():
    return render_template('index.html')

@app.route('/index')
@login_required
def index():
    return render_template('index.html')

@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        email = request.form['email']
        password = request.form['password']
        
        # Per questo esempio, accettiamo qualsiasi email e password
        # In un'applicazione reale, qui dovresti verificare le credenziali con un database
        session['email'] = email
        return redirect(url_for('home'))
    return render_template('login.html')

@app.route('/logout')
def logout():
    session.pop('email', None)
    return redirect(url_for('login'))

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0') 