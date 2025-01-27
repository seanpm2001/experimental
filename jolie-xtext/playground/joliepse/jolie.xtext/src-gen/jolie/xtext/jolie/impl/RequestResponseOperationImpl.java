/**
 * <copyright>
 * </copyright>
 *
 */
package jolie.xtext.jolie.impl;

import java.lang.String;

import java.util.Collection;

import jolie.xtext.jolie.JoliePackage;
import jolie.xtext.jolie.RequestResponseOperation;
import jolie.xtext.jolie.TypeDefinition;

import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EDataTypeEList;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Request Response Operation</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link jolie.xtext.jolie.impl.RequestResponseOperationImpl#getName <em>Name</em>}</li>
 *   <li>{@link jolie.xtext.jolie.impl.RequestResponseOperationImpl#getTypeDefinition <em>Type Definition</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class RequestResponseOperationImpl extends MinimalEObjectImpl.Container implements RequestResponseOperation
{
  /**
   * The cached value of the '{@link #getName() <em>Name</em>}' attribute list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getName()
   * @generated
   * @ordered
   */
  protected EList<String> name;

  /**
   * The cached value of the '{@link #getTypeDefinition() <em>Type Definition</em>}' containment reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getTypeDefinition()
   * @generated
   * @ordered
   */
  protected EList<TypeDefinition> typeDefinition;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected RequestResponseOperationImpl()
  {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  protected EClass eStaticClass()
  {
    return JoliePackage.Literals.REQUEST_RESPONSE_OPERATION;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EList<String> getName()
  {
    if (name == null)
    {
      name = new EDataTypeEList<String>(String.class, this, JoliePackage.REQUEST_RESPONSE_OPERATION__NAME);
    }
    return name;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EList<TypeDefinition> getTypeDefinition()
  {
    if (typeDefinition == null)
    {
      typeDefinition = new EObjectContainmentEList<TypeDefinition>(TypeDefinition.class, this, JoliePackage.REQUEST_RESPONSE_OPERATION__TYPE_DEFINITION);
    }
    return typeDefinition;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs)
  {
    switch (featureID)
    {
      case JoliePackage.REQUEST_RESPONSE_OPERATION__TYPE_DEFINITION:
        return ((InternalEList<?>)getTypeDefinition()).basicRemove(otherEnd, msgs);
    }
    return super.eInverseRemove(otherEnd, featureID, msgs);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType)
  {
    switch (featureID)
    {
      case JoliePackage.REQUEST_RESPONSE_OPERATION__NAME:
        return getName();
      case JoliePackage.REQUEST_RESPONSE_OPERATION__TYPE_DEFINITION:
        return getTypeDefinition();
    }
    return super.eGet(featureID, resolve, coreType);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @SuppressWarnings("unchecked")
  @Override
  public void eSet(int featureID, Object newValue)
  {
    switch (featureID)
    {
      case JoliePackage.REQUEST_RESPONSE_OPERATION__NAME:
        getName().clear();
        getName().addAll((Collection<? extends String>)newValue);
        return;
      case JoliePackage.REQUEST_RESPONSE_OPERATION__TYPE_DEFINITION:
        getTypeDefinition().clear();
        getTypeDefinition().addAll((Collection<? extends TypeDefinition>)newValue);
        return;
    }
    super.eSet(featureID, newValue);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public void eUnset(int featureID)
  {
    switch (featureID)
    {
      case JoliePackage.REQUEST_RESPONSE_OPERATION__NAME:
        getName().clear();
        return;
      case JoliePackage.REQUEST_RESPONSE_OPERATION__TYPE_DEFINITION:
        getTypeDefinition().clear();
        return;
    }
    super.eUnset(featureID);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public boolean eIsSet(int featureID)
  {
    switch (featureID)
    {
      case JoliePackage.REQUEST_RESPONSE_OPERATION__NAME:
        return name != null && !name.isEmpty();
      case JoliePackage.REQUEST_RESPONSE_OPERATION__TYPE_DEFINITION:
        return typeDefinition != null && !typeDefinition.isEmpty();
    }
    return super.eIsSet(featureID);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public String toString()
  {
    if (eIsProxy()) return super.toString();

    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (name: ");
    result.append(name);
    result.append(')');
    return result.toString();
  }

} //RequestResponseOperationImpl
