import bcrypt
plain = "Staff@123".encode()
salt = bcrypt.gensalt(rounds=10)
hash_value = bcrypt.hashpw(plain, salt).decode()
print(hash_value)
