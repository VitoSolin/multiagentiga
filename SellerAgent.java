import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SellerAgent that implements both static and dynamic pricing protocols.
 * Static protocol: Provides fixed base prices 
 * Dynamic protocol: Adjusts prices based on market conditions
 * 
 * This agent also opens a socket connection for Python clients.
 */
public class SellerAgent extends Agent {
    // Base prices (static protocol data)
    private Map<String, Double> basePrice = new HashMap<>();
    
    // Market demand factors (dynamic protocol data)
    private Map<String, Double> marketDemand = new HashMap<>();
    
    // Socket server for Python communication
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private final int PORT = 5555;
    private boolean running = true;

    @Override
    protected void setup() {
        System.out.println("SellerAgent " + getAID().getName() + " sudah siap dan aktif!");
        
        // Initialize product data with base prices
        basePrice.put("laptop", 15000000.0);
        basePrice.put("smartphone", 7500000.0);
        basePrice.put("headphones", 1500000.0);
        
        // Initialize market demand factors for dynamic pricing
        Random rand = new Random();
        for (String item : basePrice.keySet()) {
            // Random factor between 0.8 and 1.2
            marketDemand.put(item, 0.8 + (rand.nextDouble() * 0.4));
        }
        
        // Register the agent as a seller service
        registerSeller();
        
        // Initialize both pricing protocol behaviors
        initializePricingProtocols();
        
        // Setup socket server for Python communication
        setupSocketServer();
    }

    @Override
    protected void takeDown() {
        System.out.println("SellerAgent " + getAID().getName() + " berhenti beroperasi. Sampai jumpa!");
        closeSocketServer();
    }
    
    /**
     * Register the agent as a seller in the system
     */
    private void registerSeller() {
        // This would register the agent in a real scenario
        System.out.println("SellerAgent berhasil terdaftar di platform, siap melayani!");
    }
    
    /**
     * Initialize both the static and dynamic pricing protocol behaviors
     */
    private void initializePricingProtocols() {
        // Add static price protocol behavior
        addBehaviour(new StaticPriceProtocolBehaviour());
        System.out.println("Protokol harga statis sudah aktif, keren!");
        
        // Add dynamic price protocol behavior
        addBehaviour(new DynamicPriceProtocolBehaviour());
        System.out.println("Protokol harga dinamis sudah aktif, mantap!");
    }
    
    /**
     * Setup the socket server for Python client connections
     */
    private void setupSocketServer() {
        executor = Executors.newCachedThreadPool();
        
        // Start the socket server in a separate thread
        executor.submit(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                System.out.println("Server socket sudah nyala di port " + PORT + ", gas!");
                
                // Accept client connections
                acceptConnections();
            } catch (IOException e) {
                System.err.println("Duh, ada error saat mulai server socket: " + e.getMessage());
            }
        });
    }
    
    /**
     * Accept client connections and handle them
     */
    private void acceptConnections() {
        try {
            while (running && !serverSocket.isClosed()) {
                System.out.println("Nunggu client Python connect... Sabar ya!");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client terhubung dari: " + clientSocket.getInetAddress() + ", asik!");
                
                // Handle this client connection in a separate thread
                executor.submit(() -> handleClientConnection(clientSocket));
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Hmm, ada masalah saat terima koneksi: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handle a client connection
     */
    private void handleClientConnection(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Pesan masuk dari Python: " + inputLine);
                
                // Parse the message to determine protocol and query
                String[] parts = inputLine.split(":");
                if (parts.length >= 3) {
                    String protocol = parts[0];
                    String queryType = parts[1];
                    String item = parts[2];
                    
                    // Process based on protocol type
                    if (protocol.equals("STATIC") && queryType.equals("PRICE")) {
                        // Static protocol - return base price
                        if (basePrice.containsKey(item)) {
                            double price = basePrice.get(item);
                            out.println("STATIC:PRICE:" + item + ":" + price);
                            System.out.println("Ngirim harga statis untuk " + item + ": Rp " + String.format("%,.0f", price) + ", cuss!");
                        } else {
                            out.println("ERROR:Item not found");
                        }
                    } 
                    else if (protocol.equals("DYNAMIC") && queryType.equals("PRICE")) {
                        // Dynamic protocol - calculate current price
                        if (basePrice.containsKey(item) && marketDemand.containsKey(item)) {
                            // Calculate current price based on market demand
                            double currentPrice = basePrice.get(item) * marketDemand.get(item);
                            out.println("DYNAMIC:PRICE:" + item + ":" + currentPrice);
                            System.out.println("Ngirim harga dinamis untuk " + item + ": Rp " + String.format("%,.0f", currentPrice) + ", fresh banget!");
                            
                            // Update market demand
                            updateMarketDemand(item);
                        } else {
                            out.println("ERROR:Item not found");
                        }
                    }
                    else {
                        out.println("ERROR:Invalid protocol or query");
                    }
                } else {
                    out.println("ERROR:Invalid message format");
                }
            }
        } catch (IOException e) {
            System.err.println("Wah, error saat komunikasi dengan client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("Client udah disconnect, dadah!");
            } catch (IOException e) {
                System.err.println("Ada masalah saat nutup koneksi client: " + e.getMessage());
            }
        }
    }
    
    /**
     * Update market demand for an item (simulate market changes)
     */
    private void updateMarketDemand(String item) {
        if (marketDemand.containsKey(item)) {
            Random rand = new Random();
            // Small adjustment between 0.95 and 1.05
            double factor = 0.95 + (rand.nextDouble() * 0.1);
            double newDemand = marketDemand.get(item) * factor;
            
            // Ensure market demand stays within reasonable bounds
            if (newDemand < 0.7) newDemand = 0.7;
            if (newDemand > 1.3) newDemand = 1.3;
            
            marketDemand.put(item, newDemand);
            System.out.println("Permintaan pasar untuk " + item + " diupdate jadi " + newDemand + ", kondisi pasar berubah nih!");
        }
    }
    
    /**
     * Close the socket server
     */
    private void closeSocketServer() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("Server socket udah ditutup, bye!");
            } catch (IOException e) {
                System.err.println("Waduh, error saat nutup server socket: " + e.getMessage());
            }
        }
        
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * Behavior for static price protocol
     */
    private class StaticPriceProtocolBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Create message template for static price protocol
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                String content = msg.getContent();
                
                // Parse message for static price request
                if (content.startsWith("STATIC_PRICE")) {
                    String[] parts = content.split(":");
                    if (parts.length >= 2) {
                        String item = parts[1];
                        
                        // Send reply with base price
                        ACLMessage reply = msg.createReply();
                        if (basePrice.containsKey(item)) {
                            reply.setPerformative(ACLMessage.INFORM);
                            reply.setContent("STATIC_PRICE_RESPONSE:" + item + ":" + basePrice.get(item));
                            System.out.println("Lagi ngirim harga statis untuk " + item + ": Rp " + String.format("%,.0f", basePrice.get(item)) + ", sesuai katalog!");
                        } else {
                            reply.setPerformative(ACLMessage.REFUSE);
                            reply.setContent("Item not found");
                        }
                        
                        myAgent.send(reply);
                    }
                }
            } else {
                // If no message, block until next message arrives
                block();
            }
        }
    }
    
    /**
     * Behavior for dynamic price protocol
     */
    private class DynamicPriceProtocolBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // Create message template for dynamic price protocol
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF);
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                String content = msg.getContent();
                
                // Parse message for dynamic price request
                if (content.startsWith("DYNAMIC_PRICE")) {
                    String[] parts = content.split(":");
                    if (parts.length >= 2) {
                        String item = parts[1];
                        
                        // Send reply with current market price
                        ACLMessage reply = msg.createReply();
                        if (basePrice.containsKey(item) && marketDemand.containsKey(item)) {
                            // Calculate current price based on market demand
                            double currentPrice = basePrice.get(item) * marketDemand.get(item);
                            
                            reply.setPerformative(ACLMessage.INFORM);
                            reply.setContent("DYNAMIC_PRICE_RESPONSE:" + item + ":" + currentPrice);
                            System.out.println("Lagi ngirim harga dinamis untuk " + item + ": Rp " + String.format("%,.0f", currentPrice) + ", harga terbaru nih!");
                            
                            // Update market demand
                            updateMarketDemand(item);
                        } else {
                            reply.setPerformative(ACLMessage.REFUSE);
                            reply.setContent("Item not found");
                        }
                        
                        myAgent.send(reply);
                    }
                }
            } else {
                // If no message, block until next message arrives
                block();
            }
        }
    }
} 