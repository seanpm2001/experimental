/**
 * <copyright>
 * </copyright>
 *
 */
package jolie.xtext.jolie;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Main</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link jolie.xtext.jolie.Main#getMainrocess <em>Mainrocess</em>}</li>
 * </ul>
 * </p>
 *
 * @see jolie.xtext.jolie.JoliePackage#getMain()
 * @model
 * @generated
 */
public interface Main extends EObject
{
  /**
   * Returns the value of the '<em><b>Mainrocess</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Mainrocess</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Mainrocess</em>' containment reference.
   * @see #setMainrocess(MainProcess)
   * @see jolie.xtext.jolie.JoliePackage#getMain_Mainrocess()
   * @model containment="true"
   * @generated
   */
  MainProcess getMainrocess();

  /**
   * Sets the value of the '{@link jolie.xtext.jolie.Main#getMainrocess <em>Mainrocess</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Mainrocess</em>' containment reference.
   * @see #getMainrocess()
   * @generated
   */
  void setMainrocess(MainProcess value);

} // Main
