import socket
import sys
import time
import locale

def main():
    """
    Python client for e-commerce system that communicates with JADE SellerAgent.
    This client demonstrates how to combine static and dynamic protocols
    to make purchasing decisions.
    """
    # Set locale for Indonesian Rupiah formatting
    locale.setlocale(locale.LC_ALL, '')
    
    HOST = 'localhost'  # The JADE server hostname
    PORT = 5555        # The port used by the SellerAgent
    
    print("Client Python untuk Sistem E-commerce JADE")
    print("==========================================")
    print("Client ini pakai protokol statis dan dinamis untuk keputusan belanja")
    
    try:
        # Connect to the JADE SellerAgent
        print(f"Konek ke SellerAgent JADE di {HOST}:{PORT}...")
        client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        client_socket.connect((HOST, PORT))
        print("Berhasil terkoneksi, mantap!")
        
        # Items to consider purchasing
        shopping_list = ["laptop", "smartphone", "headphones"]
        budget = 30000000  # 2000 * 15000 = 30,000,000
        total_spent = 0
        purchased_items = []
        
        print("\nMulai proses belanja pakai dua protokol...")
        
        for item in shopping_list:
            print(f"\nLagi lihat-lihat {item}:")
            
            # Step 1: Use static protocol to get base price (reference price)
            print(f"  Pakai protokol STATIS untuk dapat harga dasar {item}")
            static_request = f"STATIC:PRICE:{item}\n"
            client_socket.sendall(static_request.encode())
            
            # Receive and parse response
            static_response = client_socket.recv(1024).decode().strip()
            print(f"  Diterima: {static_response}")
            
            if static_response.startswith("ERROR"):
                print(f"  Error: {static_response}")
                continue
                
            # Parse base price from response
            base_price = float(static_response.split(":")[-1])
            print(f"  Harga dasar (protokol statis): Rp {base_price:,.0f}")
            
            # Step 2: Use dynamic protocol to get current market price
            print(f"  Pakai protokol DINAMIS untuk dapat harga pasar {item}")
            dynamic_request = f"DYNAMIC:PRICE:{item}\n"
            client_socket.sendall(dynamic_request.encode())
            
            # Receive and parse response
            dynamic_response = client_socket.recv(1024).decode().strip()
            print(f"  Diterima: {dynamic_response}")
            
            if dynamic_response.startswith("ERROR"):
                print(f"  Error: {dynamic_response}")
                continue
                
            # Parse current price from response
            current_price = float(dynamic_response.split(":")[-1])
            print(f"  Harga terkini (protokol dinamis): Rp {current_price:,.0f}")
            
            # Step 3: Make purchasing decision by combining both protocols
            if current_price <= base_price * 1.1:
                # Buy if price is at most 10% higher than base price
                if total_spent + current_price <= budget:
                    print(f"  KEPUTUSAN: Beli {item} harga Rp {current_price:,.0f}, worth it!")
                    purchased_items.append(item)
                    total_spent += current_price
                else:
                    print(f"  KEPUTUSAN: Ga bisa beli {item}, budget ga cukup nih")
            else:
                print(f"  KEPUTUSAN: Skip beli {item}, harganya kemahalan dibanding harga dasar")
                
            # Pause between items
            time.sleep(1)
            
        # Shopping complete, display summary
        print("\nBelanja selesai!")
        print(f"Item yang dibeli: {purchased_items}")
        print(f"Total belanja: Rp {total_spent:,.0f}")
        print(f"Sisa budget: Rp {budget - total_spent:,.0f}")
        
    except ConnectionRefusedError:
        print("ERROR: Ga bisa konek ke SellerAgent JADE.")
        print("Pastiin platform JADE udah running dengan SellerAgent.")
        sys.exit(1)
    except Exception as e:
        print(f"ERROR: {e}")
        sys.exit(1)
    finally:
        try:
            # Close the socket connection
            client_socket.close()
            print("\nDisconnect dari SellerAgent JADE, makasih!")
        except:
            pass

if __name__ == "__main__":
    main() 