import React, {useState} from "react";
import {Formik} from "formik";
import {useTranslation} from "react-i18next";
import cn from 'classnames';
import NewUserGeneralTab from "./NewUserGeneralTab";
import NewUserRolesTab from "./NewUserRolesTab";
import {NewUserSchema} from "../../../shared/wizard/validate";
import {initialFormValuesNewUser} from "../../../../configs/wizardConfig";
import {getUsernames} from "../../../../selectors/userSelectors";
import {connect} from "react-redux";
import {postNewUser} from "../../../../thunks/userThunks";

/**
 * This component renders the new user wizard
 */
const NewUserWizard = ({ close, usernames }) => {
    const { t } = useTranslation();

    const [tab, setTab] = useState(0);

    const openTab = tabNr=> {
        setTab(tabNr);
    };

    const handleSubmit = values => {
        const response = postNewUser(values);
        close();
    }

    const navStyle = {
        left: '0px',
        top: 'auto',
        position: 'initial'
    };

    return (
        <>
            {/*Head navigation*/}
            <nav className="modal-nav" id="modal-nav" style={navStyle}>
                <a className={cn("wider", {active: tab === 0})}
                   onClick={() => openTab(0)}>
                    {t('USERS.USERS.DETAILS.TABS.USER')}
                </a>
                <a className={cn("wider", {active: tab === 1})}
                   onClick={() => openTab(1)}
                   title={t('USERS.USERS.DETAILS.DESCRIPTION.ROLES')}>
                    {t('USERS.USERS.DETAILS.TABS.ROLES')}
                </a>
            </nav>

            {/* Initialize overall form */}
            <Formik initialValues={initialFormValuesNewUser}
                    validationSchema={NewUserSchema(usernames)}
                    onSubmit={values => handleSubmit(values)}>

                {/* Render wizard tabs depending on current value of tab variable */}
                {formik => (
                    <>
                        {tab === 0 && (
                            <NewUserGeneralTab formik={formik}/>
                        )}
                        {tab === 1 && (
                            <NewUserRolesTab formik={formik}/>
                        )}

                        {/* Navigation buttons and validation */}
                        <footer>
                            <button className={cn("submit", {
                                active: (formik.dirty && formik.isValid),
                                inactive: !(formik.dirty && formik.isValid)})}
                                    disabled={!(formik.dirty && formik.isValid)}
                                    onClick={() => formik.handleSubmit()}>
                                {t('SUBMIT')}
                            </button>
                            <button className="cancel" onClick={() => close()}>
                                {t('CANCEL')}
                            </button>
                        </footer>
                    </>
                )}
            </Formik>
        </>
    )
};

const mapStateToProps = state => ({
    usernames: getUsernames(state)
});

export default connect(mapStateToProps)(NewUserWizard);