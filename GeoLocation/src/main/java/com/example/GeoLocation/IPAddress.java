package com.example.GeoLocation;

import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ip2location.IP2Location;
import com.ip2location.IPResult;
import com.neovisionaries.i18n.CountryCode;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/ip")
public class IPAddress {

    private static final Map<String, String> COUNTRY_NAME_TO_CODE;

    static {
        COUNTRY_NAME_TO_CODE = new HashMap<>();
        for (CountryCode code : CountryCode.values()) {
            COUNTRY_NAME_TO_CODE.put(code.getName().toLowerCase(), code.getAlpha2());
        }
    }

    @GetMapping("getiplocation")
    public String getLocation(HttpServletRequest request) {
        StringBuilder response = new StringBuilder();

        // Extract user information
        String userAgent = request.getHeader("User-Agent");
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = currentDateTime.format(formatter);

        // Get client public IP address
        String clientIp = getClientIp(request);
        String clientHostname = resolveHostname(clientIp);

        // Append client information to the response
        response.append("User Agent: ").append(userAgent).append("\n");
        response.append("Date & Time: ").append(formattedDateTime).append("\n");
        response.append("Client IP Address: ").append(clientIp).append("\n");
        response.append("Client Hostname: ").append(clientHostname).append("\n");

        try {
            // Load the IP2Location database file
            String databasePath = new ClassPathResource("IP2LOCATION-LITE-DB11.BIN").getFile().getPath();
            IP2Location ip2location = new IP2Location();
            ip2location.Open(databasePath);

            // Query geolocation for the public IP
            IPResult result = ip2location.IPQuery("182.71.67.18");
            if (result != null && "OK".equals(result.getStatus())) {
                appendLocationDetails(response, result);

                // Resolve country code
                String countryName = result.getCountryLong();
                String countryCode = getCountryCodeByName(countryName);
                response.append("Country Code: ").append(countryCode).append("\n");
            } else {
                response.append("Error: Unable to fetch geolocation details.\n");
            }

        } catch (IOException e) {
            response.append("Error: Unable to access the IP database or resolve client IP.\n");
            e.printStackTrace();
        }

        return response.toString();
    }

    private String getClientIp(HttpServletRequest request) {
        // Check for X-Forwarded-For and X-Real-IP headers (for proxies and load balancers)
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("X-Real-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr(); // Fallback to remote address
        }
        // Handle multiple IPs in X-Forwarded-For
        if (clientIp != null && clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim(); // Take the first IP if multiple are provided
        }
        return clientIp;
    }

    private String resolveHostname(String ip) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ip);
            return inetAddress.getHostName();
        } catch (IOException e) {
            e.printStackTrace();
            return "Unable to resolve hostname";
        }
    }

    private String getCountryCodeByName(String countryName) {
        if (countryName == null) {
            return "Unknown";
        }
        return COUNTRY_NAME_TO_CODE.getOrDefault(countryName.toLowerCase(), "Unknown");
    }

    private void appendLocationDetails(StringBuilder response, IPResult result) {
        if (result != null && "OK".equals(result.getStatus())) {
            response.append("Country: ").append(defaultString(result.getCountryLong())).append("\n");
            response.append("Region: ").append(defaultString(result.getRegion())).append("\n");
            response.append("City: ").append(defaultString(result.getCity())).append("\n");
            response.append("Latitude: ").append(defaultString(result.getLatitude())).append("\n");
            response.append("Longitude: ").append(defaultString(result.getLongitude())).append("\n");
            response.append("ISP: ").append(defaultString(result.getISP())).append("\n");
            response.append("Domain: ").append(defaultString(result.getDomain())).append("\n");
            response.append("Net Speed: ").append(defaultString(result.getNetSpeed())).append("\n");
            response.append("Status: ").append(defaultString(result.getStatus())).append("\n");
        } else {
            response.append("Error: Unable to fetch location details. Status: ").append(result.getStatus()).append("\n");
        }
    }

    private String defaultString(Object value) {
        return value != null ? value.toString() : "N/A";
    }
}
