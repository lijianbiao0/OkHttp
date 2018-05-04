package com.biao;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Route;
import okio.BufferedSink;

public class Client {
	public static void main(String[] args) throws Exception {
		//https://github.com/square/okhttp/wiki/Recipes 均来自官方博客
		// new Client().get();
		// new Client().asynGet();
		// new Client().accessingHeaders();
		// new Client().post();
		// new Client().postStream();
		// new Client().postFile();
		// new Client().formPost();
		// new Client().uploadFile();
		// new Client().json();
		// new Client().CacheResponse(new File(""));
		// new Client().cancel();
		// new Client().timeout();
		// new Client().nobuilder();
		new Client().authentication();

	}

	public void Authenticate() {
		client = new OkHttpClient.Builder().authenticator(new Authenticator() {
			@Override
			public Request authenticate(Route route, Response response) throws IOException {
				if (response.request().header("Authorization") != null) {
					return null; // Give up, we've already attempted to authenticate.
				}
				System.out.println("Authenticating for response: " + response);
				System.out.println("Challenges: " + response.challenges());
				String credential = Credentials.basic("jesse", "password1");
				if (credential.equals(response.request().header("Authorization"))) {
					return null; // If we already failed with these credentials, don't retry.
				}
				if (responseCount(response) >= 3) {
					return null; // If we've failed 3 times, give up.
				}

				return response.request().newBuilder().header("Authorization", credential).build();
			}

			private int responseCount(Response response) {
				int result = 1;
				while ((response = response.priorResponse()) != null) {
					result++;
				}
				return result;
			}
		}).build();
	}

	public void authentication() throws Exception {
		Authenticate();
		Request request = new Request.Builder().url("http://publicobject.com/secrets/hellosecret.txt").build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful())
				throw new IOException("Unexpected code " + response);

			System.out.println(response.body().string());
		}
	}

	// 每次调用都会生成新的客户端
	public void nobuilder() throws Exception {
		Request request = new Request.Builder().url("http://httpbin.org/delay/1").build(); // This URL is served with a
																							// 1 second delay.

		// Copy to customize OkHttp for this request.
		OkHttpClient client1 = client.newBuilder().readTimeout(500, TimeUnit.MILLISECONDS).build();
		try (Response response = client1.newCall(request).execute()) {
			System.out.println("Response 1 succeeded: " + response);
		} catch (IOException e) {
			System.out.println("Response 1 failed: " + e);
		}

		// Copy to customize OkHttp for this request.
		OkHttpClient client2 = client.newBuilder().readTimeout(3000, TimeUnit.MILLISECONDS).build();
		try (Response response = client2.newCall(request).execute()) {
			System.out.println("Response 2 succeeded: " + response);
		} catch (IOException e) {
			System.out.println("Response 2 failed: " + e);
		}
	}

	public void ConfigureTimeouts() throws Exception {
		client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS)
				.readTimeout(30, TimeUnit.SECONDS).build();
	}

	public void timeout() throws Exception {
		ConfigureTimeouts();
		Request request = new Request.Builder().url("http://httpbin.org/delay/2") // This URL is served with a 2 second
																					// delay.
				.build();

		try (Response response = client.newCall(request).execute()) {
			System.out.println("Response completed: " + response);
		}
	}

	private OkHttpClient client = new OkHttpClient();

	public void get() throws Exception {
		Request request = new Request.Builder().url("https://publicobject.com/helloworld.txt").build();

		try {
			Response response = client.newCall(request).execute();
			if (!response.isSuccessful())
				throw new IOException("Unexpected code " + response);

			Headers responseHeaders = response.headers();
			for (int i = 0; i < responseHeaders.size(); i++) {
				System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
			}

			System.out.println(response.body().string());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void asynGet() throws Exception {
		Request request = new Request.Builder().url("http://publicobject.com/helloworld.txt").build();

		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				e.printStackTrace();
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				try (ResponseBody responseBody = response.body()) {
					if (!response.isSuccessful())
						throw new IOException("Unexpected code " + response);

					Headers responseHeaders = response.headers();
					for (int i = 0, size = responseHeaders.size(); i < size; i++) {
						System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
					}

					System.out.println(responseBody.string());
				}
			}
		});
	}

	public void accessingHeaders() throws Exception {
		Request request = new Request.Builder().url("https://api.github.com/repos/square/okhttp/issues")
				.header("User-Agent", "OkHttp Headers.java").addHeader("Accept", "application/json; q=0.5")
				.addHeader("Accept", "application/vnd.github.v3+json").build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful())
				throw new IOException("Unexpected code " + response);

			System.out.println("Server: " + response.header("Server"));
			System.out.println("Date: " + response.header("Date"));
			System.out.println("Vary: " + response.headers("Vary"));
		}
	}

	public static final MediaType MEDIA_TYPE_MARKDOWN = MediaType.parse("text/x-markdown; charset=utf-8");

	public void post() throws Exception {
		String postBody = "Releases\n" + "--------\n" + "\n" + " * _1.0_ May 6, 2013\n" + " * _1.1_ June 15, 2013\n"
				+ " * _1.2_ August 11, 2013\n";
		System.out.println(postBody);

		Request request = new Request.Builder().url("https://api.github.com/markdown/raw")
				.post(RequestBody.create(MEDIA_TYPE_MARKDOWN, postBody)).build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful())
				throw new IOException("Unexpected code " + response);

			System.out.println(response.body().string());
		}
	}

	public void postStream() throws Exception {
		RequestBody requestBody = new RequestBody() {
			@Override
			public MediaType contentType() {
				return MEDIA_TYPE_MARKDOWN;
			}

			@Override
			public void writeTo(BufferedSink sink) throws IOException {
				sink.writeUtf8("Numbers\n");
				sink.writeUtf8("-------\n");
				for (int i = 2; i <= 997; i++) {
					sink.writeUtf8(String.format(" * %s = %s\n", i, factor(i)));
				}
			}

			private String factor(int n) {
				for (int i = 2; i < n; i++) {
					int x = n / i;
					if (x * i == n)
						return factor(x) + " × " + i;
				}
				return Integer.toString(n);
			}
		};

		Request request = new Request.Builder().url("https://api.github.com/markdown/raw").post(requestBody).build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful())
				throw new IOException("Unexpected code " + response);

			System.out.println(response.body().string());
		}
	}

	public void postFile() throws Exception {
		File file = new File("README.md");

		Request request = new Request.Builder().url("https://api.github.com/markdown/raw")
				.post(RequestBody.create(MEDIA_TYPE_MARKDOWN, file)).build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful())
				throw new IOException("Unexpected code " + response);

			System.out.println(response.body().string());
		}
	}

	public void formPost() throws Exception {
		RequestBody formBody = new FormBody.Builder().add("search", "Jurassic Park").build();
		Request request = new Request.Builder().url("https://en.wikipedia.org/w/index.php").post(formBody).build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful())
				throw new IOException("Unexpected code " + response);

			System.out.println(response.body().string());
		}
	}

	private static final String IMGUR_CLIENT_ID = "...";
	private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");

	public void uploadFile() throws Exception {
		// Use the imgur image upload API as documented at
		// https://api.imgur.com/endpoints/image
		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("title", "Square Logo")
				.addFormDataPart("image", "logo-square.png", RequestBody.create(MEDIA_TYPE_PNG, new File("README.md")))
				.build();

		Request request = new Request.Builder().header("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
				.url("https://api.imgur.com/3/image").post(requestBody).build();

		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful())
				throw new IOException("Unexpected code " + response);

			System.out.println(response.body().string());
		}
	}

	public void json() throws Exception {
		Request request = new Request.Builder().url("https://api.github.com/gists/c2a7c39532239ff261be").build();
		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful())
				throw new IOException("Unexpected code " + response);

			System.out.println(response.body().string());
			;// Json

		}
	}

	public void CacheResponse(File cacheDirectory) throws Exception {
		int cacheSize = 10 * 1024 * 1024; // 10 MiB
		Cache cache = new Cache(cacheDirectory, cacheSize);

		client = new OkHttpClient.Builder().cache(cache).build();

		Request request = new Request.Builder().url("http://publicobject.com/helloworld.txt").build();

		String response1Body;
		try (Response response1 = client.newCall(request).execute()) {
			if (!response1.isSuccessful())
				throw new IOException("Unexpected code " + response1);

			response1Body = response1.body().string();
			System.out.println("Response 1 response:          " + response1);
			System.out.println("Response 1 cache response:    " + response1.cacheResponse());
			System.out.println("Response 1 network response:  " + response1.networkResponse());
		}

		String response2Body;
		try (Response response2 = client.newCall(request).execute()) {
			if (!response2.isSuccessful())
				throw new IOException("Unexpected code " + response2);

			response2Body = response2.body().string();
			System.out.println("Response 2 response:          " + response2);
			System.out.println("Response 2 cache response:    " + response2.cacheResponse());
			System.out.println("Response 2 network response:  " + response2.networkResponse());
		}

		System.out.println("Response 2 equals Response 1? " + response1Body.equals(response2Body));
	}

	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	public void cancel() throws Exception {
		Request request = new Request.Builder().url("http://httpbin.org/delay/2") // This URL is served with a 2 second
																					// delay.
				.build();

		final long startNanos = System.nanoTime();
		final Call call = client.newCall(request);

		// Schedule a job to cancel the call in 1 second.
		executor.schedule(new Runnable() {
			@Override
			public void run() {
				System.out.printf("%.2f Canceling call.%n", (System.nanoTime() - startNanos) / 1e9f);
				call.cancel();
				System.out.printf("%.2f Canceled call.%n", (System.nanoTime() - startNanos) / 1e9f);
			}
		}, 1, TimeUnit.SECONDS);

		System.out.printf("%.2f Executing call.%n", (System.nanoTime() - startNanos) / 1e9f);
		try (Response response = call.execute()) {
			System.out.printf("%.2f Call was expected to fail, but completed: %s%n",
					(System.nanoTime() - startNanos) / 1e9f, response);
		} catch (IOException e) {
			System.out.printf("%.2f Call failed as expected: %s%n", (System.nanoTime() - startNanos) / 1e9f, e);
		}
	}

}
