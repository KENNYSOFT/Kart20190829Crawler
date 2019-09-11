package kr.KENNYSOFT.Kart20190829Crawler;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Kart20190829Crawler
{
	public static JSONParser jsonParser = new JSONParser();
	public static final Pattern pattern = Pattern.compile(" \\((\\d+)개\\)$");
	public static final String ENC = "YOUR_ENC";
	public static final String KENC = "YOUR_KENC";
	public static final String NPP = "YOUR_NPP";
	
	public static void main(String[] args) throws Exception
	{
		CookieManager cookieManager = new CookieManager();
		CookieHandler.setDefault(cookieManager);
		HttpCookie cookie1 = new HttpCookie("ENC", ENC);
		cookie1.setDomain("nexon.com");
		cookie1.setPath("/");
		cookie1.setVersion(0);
		cookieManager.getCookieStore().add(new URI("http://kart.nexon.com/"), cookie1);
		HttpCookie cookie2 = new HttpCookie("KENC", KENC);
		cookie2.setDomain("kart.nexon.com");
		cookie2.setPath("/");
		cookie2.setVersion(0);
		cookieManager.getCookieStore().add(new URI("http://kart.nexon.com/"), cookie2);
		HttpCookie cookie3 = new HttpCookie("NPP", NPP);
		cookie3.setDomain("nexon.com");
		cookie3.setPath("/");
		cookie3.setVersion(0);
		cookieManager.getCookieStore().add(new URI("http://kart.nexon.com/"), cookie3);
		CSVPrinter csvPrinter = new CSVPrinter(Files.newBufferedWriter(Paths.get("coupon.csv")), CSVFormat.DEFAULT.withHeader("\uFEFF아이템", "수량", "당첨일시", "유효기간", "쿠폰번호"));
		while (true)
		{
			boolean success = false;
			boolean completed;
			do
			{
				completed = true;
				try
				{
					success = play();
				}
				catch (HttpRetryException e)
				{
					cookieManager.getCookieStore().add(new URI("http://kart.nexon.com/"), cookie1);
					cookieManager.getCookieStore().add(new URI("http://kart.nexon.com/"), cookie2);
					cookieManager.getCookieStore().add(new URI("http://kart.nexon.com/"), cookie3);
					completed = false;
				}
			} while (!completed);
			if (!success) break;
		}
		int pages = 1;
		for (int i = 1; i <= pages; ++i)
		{
			JSONObject object = null;
			boolean completed;
			do
			{
				completed = true;
				try
				{
					object = list(i);
				}
				catch (HttpRetryException e)
				{
					cookieManager.getCookieStore().add(new URI("http://kart.nexon.com/"), cookie1);
					cookieManager.getCookieStore().add(new URI("http://kart.nexon.com/"), cookie2);
					cookieManager.getCookieStore().add(new URI("http://kart.nexon.com/"), cookie3);
					completed = false;
				}
			} while (!completed);
			pages = ((int) (long) object.get("n4totCnt") + 5) / 6;
			JSONArray array = (JSONArray) object.get("strCouponList");
			for (Object obj : array)
			{
				JSONObject item = (JSONObject) obj;
				Matcher matcher = pattern.matcher((String) item.get("strItemName"));
				csvPrinter.printRecord(((String) item.get("strItemName")).replaceAll(" \\(\\d+개\\)$", ""), matcher.find() ? matcher.group(1) : "-", item.get("dtCreate"), "2019-09-30 23:59", item.get("strCoupon"));
			}
		}
		csvPrinter.flush();
		csvPrinter.close();
	}
	
	public static boolean play() throws Exception
	{
		HttpURLConnection conn = (HttpURLConnection) new URL("http://kart.nexon.com/events/2019/0829/Play.aspx").openConnection();
		conn.setRequestMethod("POST");
		conn.setFixedLengthStreamingMode(0);
		conn.setRequestProperty("Referer", "http://kart.nexon.com/events/2019/0829/event.aspx");
		conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
		conn.setDoOutput(true);
		DataOutputStream os = new DataOutputStream(conn.getOutputStream());
		os.flush();
		os.close();
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		StringBuffer response = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) response.append(line);
		br.close();
		conn.disconnect();
		System.out.println(response);
		try
		{
			return (long) ((JSONObject) jsonParser.parse(response.toString())).get("retCode") == 0;
		}
		catch (Exception e)
		{
			throw new HttpRetryException("", 0);
		}
	}
	
	public static JSONObject list(int page) throws Exception
	{
		HttpURLConnection conn = (HttpURLConnection) new URL("http://kart.nexon.com/events/2019/0829/MyCouponList.aspx").openConnection();
		Map<String, String> parameters = new HashMap<>();
		parameters.put("n4Page", String.valueOf(page));
		StringJoiner sj = new StringJoiner("&");
		for (Entry<String, String> entry : parameters.entrySet()) sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8"));
		byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
		conn.setRequestMethod("POST");
		conn.setFixedLengthStreamingMode(out.length);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("Referer", "http://kart.nexon.com/events/2019/0829/event.aspx");
		conn.setDoOutput(true);
		DataOutputStream os = new DataOutputStream(conn.getOutputStream());
		os.write(out);
		os.flush();
		os.close();
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
		StringBuffer response = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) response.append(line);
		br.close();
		conn.disconnect();
		System.out.println(response);
		return (JSONObject) jsonParser.parse(response.toString());
	}
}