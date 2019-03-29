package utils;

import com.upek.android.ptapi.struct.PtInputBir;

import java.io.Serializable;

public class FBIFingerModel  implements Serializable{
	
	private static final long serialVersionUID = 8075809222703831514L;
	private String model_ID;
	private PtInputBir template;
	
	public String getModel_ID() {
		return model_ID;
	}
	public void setModel_ID(String model_ID) {
		this.model_ID = model_ID;
	}
	public PtInputBir getTemplate() {
		return template;
	}
	public void setTemplate(PtInputBir template) {
		this.template = template;
	}
	
}
