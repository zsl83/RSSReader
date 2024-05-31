import org.jsoup.Jsoup;
import org.w3c.dom.NodeList;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Scanner;

public class RSSReader {

    private static final int MAX_ITEMS = 10; // Define maximum number of RSS items to display
    private static ArrayList<String> websitesList = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to RSS Reader");
        boolean isRunning = true;
        while (isRunning) {
            System.out.println("Type a valid number for your desired action:");
            System.out.println("[1] Show updates \n[2] Add URL\n[3] Remove URL\n[4] Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    int r = showUpdates();
                    if (r != -1) {
                        isRunning = false;
                    }
                    break;
                case 2:
                    addWebsite();
                    break;
                case 3:
                    deleteWebsite();
                    break;
                case 4:
                    saveWebsites();
                    System.out.println("Exiting program...");
                    isRunning = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number between 1 and 4.");
            }
        }
        scanner.close();
    }

    private static void addWebsite() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Please enter website URL to add: \n");
        String URL = scanner.nextLine().trim();
        if (websitesList.stream().anyMatch(url -> url.equalsIgnoreCase(URL))) {
            System.out.println(URL + " already exists.\n");
        } else {
            websitesList.add(URL);
            System.out.println("Added " + URL + " successfully");
        }
    }

    private static void deleteWebsite() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Please enter website URL to remove: ");
        String removeURL = scanner.nextLine().trim();

        boolean removed = websitesList.removeIf(website -> website.equalsIgnoreCase(removeURL));
        if (removed) {
            System.out.println("Removed " + removeURL + " successfully.");
        } else {
            System.out.println("Couldn't find " + removeURL);
        }
    }

    private static int showUpdates() throws Exception {
        if (websitesList.isEmpty()) {
            System.out.println("No websites added yet.");
        } else {
            System.out.println("Show updates for:");
            for (int i = 0; i < websitesList.size(); i++) {
                String title = extractPageTitle(fetchPageSource(websitesList.get(i)));
                System.out.println("[" + i + "] " + title);
            }
        }
        System.out.println("Enter -1 to return");
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        if (n == -1) {
            return -1;
        } else if (n >= 0 && n < websitesList.size()) {
            retrieveRssContent(websitesList.get(n));
        } else {
            System.out.println("Invalid selection.");
        }
        return n;
    }

    private static void saveWebsites() throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("data.txt"))) {
            for (String website : websitesList) {
                String title = extractPageTitle(fetchPageSource(website));
                String rssUrl = extractRssUrl(website);
                String html = fetchPageSource(website);
                writer.write(title + ";" + html + ";" + rssUrl);
                writer.newLine();
            }
            System.out.println("Websites saved to file successfully!");
        } catch (IOException e) {
            System.out.println("Error saving websites to file: " + e.getMessage());
        }
    }

    public static String extractPageTitle(String html) {
        try {
            org.jsoup.nodes.Document doc = Jsoup.parse(html);
            return doc.select("title").first().text();
        } catch (Exception e) {
            return "Error: no title tag found in page source!";
        }
    }

    public static void retrieveRssContent(String rssUrl) {
        try {
            String rssXml = fetchPageSource(rssUrl);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            ByteArrayInputStream input = new ByteArrayInputStream(rssXml.getBytes("UTF-8"));
            org.w3c.dom.Document doc = builder.parse(input);
            doc.getDocumentElement().normalize();
            NodeList itemNodes = doc.getElementsByTagName("item");

            for (int i = 0; i < Math.min(itemNodes.getLength(), MAX_ITEMS); i++) {
                org.w3c.dom.Node itemNode = itemNodes.item(i);
                if (itemNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    org.w3c.dom.Element element = (org.w3c.dom.Element) itemNode;
                    System.out.println("Title: " + getElementValue(element, "title"));
                    System.out.println("Link: " + getElementValue(element, "link"));
                    System.out.println("Description: " + getElementValue(element, "description"));
                }
            }
        } catch (Exception e) {
            System.out.println("Error in retrieving RSS content for " + rssUrl + ": " + e.getMessage());
        }
    }

    private static String getElementValue(org.w3c.dom.Element parentElement, String tagName) {
        NodeList nodeList = parentElement.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return "";
    }

    public static String extractRssUrl(String url) throws IOException {
        org.jsoup.nodes.Document doc = Jsoup.connect(url).get();
        String rssUrl = doc.select("link[type=application/rss+xml]").attr("abs:href");
        if (rssUrl.isEmpty()) {
            throw new IOException("RSS URL not found for " + url);
        }
        return rssUrl;
    }

    public static String fetchPageSource(String urlString) throws Exception {
        InputStream inputStream = null;
        try {
            URL url = new URL(urlString);
            URLConnection urlConnection = url.openConnection();
            if (urlString.toLowerCase().startsWith("https")) {
                urlConnection = (HttpsURLConnection) url.openConnection();
            } else {
                urlConnection = (HttpURLConnection) url.openConnection();
            }
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");
            inputStream = urlConnection.getInputStream();
            return toString(inputStream);
        } catch (IOException e) {
            throw new Exception("Error fetching page source: " + e.getMessage());
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private static String toString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        StringBuilder stringBuilder = new StringBuilder();
        String inputLine;
        while ((inputLine = bufferedReader.readLine()) != null) {
            stringBuilder.append(inputLine);
        }
        bufferedReader.close();
        return stringBuilder.toString();
    }
}






