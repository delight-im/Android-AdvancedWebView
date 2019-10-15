package com.advancewebview;

import android.os.Parcel;
import android.os.Parcelable;

public class HeaderObj implements Parcelable {
	public static final Creator<HeaderObj> CREATOR = new Creator<HeaderObj>() {
		@Override
		public HeaderObj createFromParcel(Parcel in) {
			return new HeaderObj(in);
		}

		@Override
		public HeaderObj[] newArray(int size) {
			return new HeaderObj[size];
		}
	};
	private String headerName;
	private String headerData;

	public HeaderObj(String headerName, String headerData) {
		this.headerName = headerName;
		this.headerData = headerData;
	}

	public HeaderObj(Parcel in) {
		this.headerName = in.readString();
		this.headerData = in.readString();
	}

	public String getHeaderName() {
		return headerName;
	}

	public void setHeaderName(String headerName) {
		this.headerName = headerName;
	}

	public String getHeaderData() {
		return headerData;
	}

	public void setHeaderData(String headerData) {
		this.headerData = headerData;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(headerName);
		dest.writeString(headerData);
	}
}
