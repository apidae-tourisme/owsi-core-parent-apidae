package fr.openwide.core.etcd.action.model.role;

import java.io.Serializable;

import fr.openwide.core.infinispan.action.SwitchRoleResult;

/**
 * Gestion des résultats de changement de rôle
 */
public class SwitchRoleResultActionResult implements Serializable {

	private static final long serialVersionUID = -4770723016176821835L;

	private SwitchRoleResult switchRoleResult;

	private String message;
	
	public SwitchRoleResultActionResult() {
		super();
	}
	
	public SwitchRoleResultActionResult(SwitchRoleResult switchRoleResult, String message) {
		this();
		this.switchRoleResult = switchRoleResult;
		this.message = message;
	}

	public SwitchRoleResult getSwitchRoleResult() {
		return switchRoleResult;
	}

	public String getMessage() {
		return message;
	}

	public void setSwitchRoleResult(SwitchRoleResult switchRoleResult) {
		this.switchRoleResult = switchRoleResult;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
