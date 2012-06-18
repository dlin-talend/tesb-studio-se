package org.talend.designer.camel.dependencies.core.model;

public class ImportPackage extends OsgiDependencies {

	@Override
	public int getType() {
		return IMPORT_PACKAGE;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);

		if (minVersion != null && maxVersion != null) {
			sb.append(";version=\"[");
			sb.append(minVersion);
			sb.append(",");
			sb.append(maxVersion);
			sb.append(")\"");
		} else if (minVersion != null) {
			sb.append(";version=\"");
			sb.append(minVersion);
			sb.append("\"");
		} else if (maxVersion != null) {
			sb.append(";version=\"");
			sb.append(maxVersion);
			sb.append("\"");
		}

		if (isOptional) {
			sb.append(";resolution:=optional");
		}

		return sb.toString();
	}

	// public static void main(String[] args) {
	// ImportPackage importPackage = new ImportPackage();
	// importPackage.setName("aaa");
	// System.out.println(importPackage);
	//
	// importPackage.setMinVersion("1.0.0");
	// System.out.println(importPackage);
	//
	// importPackage.setMaxVersion("2.0.0");
	// System.out.println(importPackage);
	//
	// importPackage.setMinVersion(null);
	// System.out.println(importPackage);
	//
	// importPackage.setMaxVersion(null);
	// System.out.println(importPackage);
	//
	// importPackage.setMinVersion("1.0.0");
	// System.out.println(importPackage);
	//
	// importPackage.setOptional(true);
	// System.out.println(importPackage);
	//
	// importPackage.setMaxVersion("2.0.0");
	// System.out.println(importPackage);
	// }
}
