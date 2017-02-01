package com.orgzly.android.provider.clients;

import android.content.Context;

import com.orgzly.android.provider.ProviderContract;

public class DbClient {
    public static void recreateTables(Context context) {
        context.getContentResolver().update(ProviderContract.DbRecreate.ContentUri.dbRecreate(), null, null, null);
    }

    public static void toTest(Context context) {
        context.getContentResolver().update(ProviderContract.DbTest.ContentUri.dbTest(), null, null, null);
    }
}
