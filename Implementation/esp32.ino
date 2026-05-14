#include <WiFi.h>

// WiFi credentials
const char* ssid = "";
const char* password = "";

// Raspberry Pi details
const char* pi_ip = "";   // 
const int pi_port = 6000;

// Server (Laptop → ESP32)
WiFiServer server(1234);

// Client (ESP32 → Pi)
WiFiClient piClient;

void setup() {
  Serial.begin(115200);
  delay(1000);

  Serial.println("Starting ESP32...");

  // Connect to WiFi
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }

  Serial.println("Connected!");
  Serial.print("ESP32 IP: ");
  Serial.println(WiFi.localIP());

  // Start server (for laptop)
  server.begin();
  Serial.println("Server started");

  // Connect to Raspberry Pi
  Serial.println("Connecting to Pi...");
  while (!piClient.connect(pi_ip, pi_port)) {
    Serial.println("Retrying Pi connection...");
    delay(1000);
  }

  Serial.println("Connected to Pi!");
}

void loop() {
  WiFiClient client = server.available();

  if (client) {
    Serial.println("Laptop connected");

    while (client.connected()) {

      if (client.available()) {
        uint8_t buffer[256];
        int len = client.read(buffer, sizeof(buffer));

        if (len > 0) {
          // Forward EXACT bytes to Pi
          piClient.write(buffer, len);

          Serial.print("Forwarded: ");
          Serial.println(len);
        }
      }
    }

    client.stop();
    Serial.println("Laptop disconnected");
  }
}