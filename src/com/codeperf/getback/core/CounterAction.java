package com.codeperf.getback.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;

import com.codeperf.getback.common.Constants;
import com.codeperf.getback.common.Utils;

public class CounterAction {

	private static final String LOG_TAG = CounterAction.class.getSimpleName();

	public static void sendSMS(Context context, String receipient,
			String messageBody, final ISMSNotifierCallback cb) {

		SMSNotifier.sendMessage(context, receipient, messageBody, cb);
	}

	public static void sendEmail(String subject, String body,
			String receipients, String filePath, ISendEmailCallback callback) {
		Utils.LogUtil.LogD(Constants.LOG_TAG, "Attachment - " + filePath);
		EmailSender emailSender = new EmailSender(
				Constants.DEFAULT_SENDER_EMAILADDRESS,
				Constants.DEFAULT_SENDER_PASSWORD, callback);
		emailSender.addAttachment(filePath);
		emailSender.sendMail(subject, body,
				Constants.DEFAULT_SENDER_EMAILADDRESS, receipients);
		return;
	}

	public static int clearAllSMS(Context context) {
		ContentResolver contentResolver = context.getContentResolver();
		Uri inboxUri = Uri.parse("content://sms");
		Cursor cursor = contentResolver.query(inboxUri, null, null, null, null);

		int count = 0;
		if (cursor != null) {
			count = cursor.getCount();
			Utils.LogUtil.LogD(LOG_TAG, "clearAllSMS : Count = " + count);
			while (cursor.moveToNext()) {
				try {
					// Delete the SMS
					String pid = cursor.getString(0); // Get id;
					String uri = "content://sms/" + pid;
					contentResolver.delete(Uri.parse(uri), null, null);
				} catch (Exception e) {
					Utils.LogUtil.LogE(LOG_TAG, "Exception : ", e);
				}
			}
		}
		return count;
	}

	public static boolean deleteSmsByAddress(Context context, String contactNo) {
		ContentResolver contentResolver = context.getContentResolver();
		Uri inboxUri = Uri.parse("content://sms/sent");
		Cursor cursor = contentResolver.query(inboxUri, null, "address = ?",
				new String[] { contactNo }, null);

		boolean bReturn = false;
		if (cursor != null) {
			Utils.LogUtil.LogD(LOG_TAG, " deleteSmsByAddress: Count = "
					+ cursor.getCount());
			while (cursor.moveToNext()) {
				try {
					// Delete the SMS
					String pid = cursor.getString(0); // Get id;
					String uri = "content://sms/" + pid;
					contentResolver.delete(Uri.parse(uri), null, null);
					bReturn = true;
				} catch (Exception e) {
					Utils.LogUtil.LogE(Constants.LOG_TAG, "Exception : ", e);
				}
			}
		}
		return bReturn;
	}

	public static int clearAllContacts(Context context) {
		Utils.LogUtil.LogD(Constants.LOG_TAG, "Inside clearAllContacts");

		ContentResolver contentResolver = context.getContentResolver();
		Cursor cursor = contentResolver.query(
				ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
		int count = 0;
		if (cursor != null) {
			count = cursor.getCount();
			while (cursor.moveToNext()) {
				String lookupKey = cursor.getString(cursor
						.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
				Uri uri = Uri
						.withAppendedPath(
								ContactsContract.Contacts.CONTENT_LOOKUP_URI,
								lookupKey);
				contentResolver.delete(uri, null, null);
			}
		}
		return count;
	}

	public static void formatExternalStorage(Context context) {

		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			File deleteMatchingFile = new File(Environment
					.getExternalStorageDirectory().toString());
			try {
				File[] filenames = deleteMatchingFile.listFiles();
				if (filenames != null && filenames.length > 0) {
					for (File tempFile : filenames) {
						if (tempFile.isDirectory()) {
							wipeDirectory(tempFile.toString());
						}
						tempFile.delete();
					}
				} else {
					deleteMatchingFile.delete();
				}
			} catch (Exception e) {
				Utils.LogUtil.LogE(LOG_TAG, "Exception: ", e);
			}
		}
	}

	private static void wipeDirectory(String name) {
		File directoryFile = new File(name);
		File[] filenames = directoryFile.listFiles();
		if (filenames != null && filenames.length > 0) {
			for (File tempFile : filenames) {
				if (tempFile.isDirectory()) {
					wipeDirectory(tempFile.toString());
				}
				tempFile.delete();
			}
		} else {
			directoryFile.delete();
		}
	}

	public static void removeAccounts(Context context) {

		AccountManager accountManager = (AccountManager) context
				.getSystemService(Context.ACCOUNT_SERVICE);
		Account[] accounts = accountManager.getAccounts();
		for (Account account : accounts) {
			try {
				accountManager.removeAccount(account, null, null);
			} catch (Exception e) {
				Utils.LogUtil.LogE(LOG_TAG, "Exception: ", e);
			}
		}
	}

	public static String backupContacts(Context context) {

		Cursor cursor = context.getContentResolver().query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null,
				null, null);
		String vcardPath = null;

		if (cursor != null && cursor.getCount() > 0) {
			vcardPath = context.getFilesDir() + File.separator
					+ "getback_contacts.vcf";
			FileOutputStream fos;
			try {
				fos = context.openFileOutput("getback_contacts.vcf",
						Context.MODE_PRIVATE);
				cursor.moveToFirst();
				for (int i = 0; i < cursor.getCount(); i++) {
					String vcardString = getVCardString(context, cursor);
					if (vcardString != null) {
						Utils.LogUtil.LogD("TAG", "Contact " + (i + 1)
								+ "VcF String is" + vcardString);
						cursor.moveToNext();
						fos.write(vcardString.getBytes());
					}
				}
				fos.flush();
				fos.close();
			} catch (Exception e) {
				Utils.LogUtil.LogE(Constants.LOG_TAG, "Exception : ", e);
				vcardPath = null;
			}
			cursor.close();
		} else {
			Utils.LogUtil.LogD("TAG", "No Contacts in Your Phone");
		}
		return vcardPath;
	}

	private static String getVCardString(Context context, Cursor cursor) {

		try {
			String lookupKey = cursor.getString(cursor
					.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
			Uri uri = Uri.withAppendedPath(
					ContactsContract.Contacts.CONTENT_VCARD_URI, lookupKey);

			AssetFileDescriptor fd = context.getContentResolver()
					.openAssetFileDescriptor(uri, "r");

			FileInputStream fis = fd.createInputStream();
			byte[] buf = new byte[(int) fd.getDeclaredLength()];
			fis.read(buf);
			fd.close();
			return new String(buf);
		} catch (Exception e) {
			Utils.LogUtil.LogE(Constants.LOG_TAG, "Exception : ", e);
		}
		return null;
	}
}
