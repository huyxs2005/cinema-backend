import bcrypt
values = [
    ("Staff@123", "$2a$10$A3AGn1nP6h4iS6yCyHkUwu5gWeK1qG1s0ZlZ1aU5vT1cs5hZVXAB6"),
    ("Staff@123", "$2a$10$96JHeD7F1R8lJ68mPR2ZtOG8.zqrr1LIsPqVAH7SaVY3B8PX0irAi")
]
for pwd, hashed in values:
    try:
        ok = bcrypt.checkpw(pwd.encode(), hashed.encode())
    except ValueError as exc:
        ok = str(exc)
    print(pwd, hashed[:20] + '...', ok)
