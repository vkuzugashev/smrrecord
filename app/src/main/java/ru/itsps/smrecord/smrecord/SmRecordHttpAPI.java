package ru.itsps.smrecord.smrecord;

import com.google.gson.annotations.SerializedName;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

/**
 * Created by penart on 08.10.2016.
 */

public interface SmRecordHttpAPI {
    @FormUrlEncoded
    @POST("conf")
    Call<Conf> GetConf(@Field("PHONE") String phone);

    @FormUrlEncoded
    @POST("sr")
    Call<ResponseBody> SaveRecord(@Field(RecordStoreContract.Record.COLUMN_NAME_STARTDT) String startdt,
                                  @Field(RecordStoreContract.Record.COLUMN_NAME_STOPDT) String stopdt,
                                  @Field(RecordStoreContract.Record.COLUMN_NAME_DIRECTION) int direction,
                                  @Field(RecordStoreContract.Record.COLUMN_NAME_DURATION) int duration,
                                  @Field(RecordStoreContract.Record.COLUMN_NAME_UNANSWERED) int unanswered,
                                  @Field("PHONE") String phone,
                                  @Field(RecordStoreContract.Record.COLUMN_NAME_REMOTE_PHONE) String remotephone,
                                  @Field(RecordStoreContract.Record.COLUMN_NAME_RECORD_FILE) String recordfile
                        );

    @Multipart
    @POST("sr2")
    Call<ResponseBody> SaveRecord2(@Part(RecordStoreContract.Record.COLUMN_NAME_STARTDT) RequestBody startdt,
                                  @Part(RecordStoreContract.Record.COLUMN_NAME_STOPDT) RequestBody stopdt,
                                  @Part(RecordStoreContract.Record.COLUMN_NAME_DIRECTION) RequestBody direction,
                                  @Part(RecordStoreContract.Record.COLUMN_NAME_DURATION) RequestBody duration,
                                  @Part(RecordStoreContract.Record.COLUMN_NAME_UNANSWERED) RequestBody unanswered,
                                  @Part("PHONE") RequestBody phone,
                                  @Part(RecordStoreContract.Record.COLUMN_NAME_REMOTE_PHONE) RequestBody remotephone,
                                  @Part(RecordStoreContract.Record.COLUMN_NAME_RECORD_FILE) RequestBody recordfile,
                                  @Part MultipartBody.Part file
    );
}

