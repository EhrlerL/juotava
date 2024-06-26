import {React, useContext, useEffect, useState} from 'react';

import { useNavigate } from 'react-router';

import { useAuth0 } from '@auth0/auth0-react';
import {baseUrlUser } from '../../../config/config';
import { AuthenticatedRequestWrapperContext } from '../../../App';

import {userSettings, Settings} from '../../../model/userSettings';

import { Button, Col, Container, FloatingLabel, Form, Row, Spinner } from 'react-bootstrap';

import ImageUploaderUser from './ImageUploaderUser';
import LogOutButton from '../../auth/LogOutButton';

function User(props) {
    let style = { width: 'auto', cursor: 'pointer', padding: '0' };
    if(props.disabled) {
        style = { width: 'auto', cursor: 'not-allowed', padding: '0', opacity: '0.5' };
    }
    const navigate = useNavigate();
    const cyClass = (props.cyClass !== undefined) ? " "+props.cyClass : "";
    const arw = useContext(AuthenticatedRequestWrapperContext);
    const {user, isAuthenticated, getAccessTokenSilently} = useAuth0();

    const handleChangeImage = (base64Image) => {
        setUserSet(prevUserSet => ({
            ...prevUserSet,
            image: {
                ...prevUserSet.image,
                base64data: base64Image
            }
        }));
    }

    const handleChangeUsername = (event) => {
        const newUsername = event.target.value;
        setUserSet(prevUserSet => ({
            ...prevUserSet,
            userName: newUsername
        }));
    }

    const handleChangeShowUsername = (event) => {
        const newShowUsername = event.target.checked;
        setUserSet(prevUserSet => ({
            ...prevUserSet,
            settings: {
                ...prevUserSet.settings,
                showUserNameInRecipe: newShowUsername
            }
        }));
    }

    const [saveUserSuccess, setSaveUserSuccess] = useState();
    const handleSave = () => {
        setSaveUserSuccess("waiting");
        arw.request({isAuthenticated, getAccessTokenSilently}, baseUrlUser, 'user/save', 'POST', JSON.stringify(userSet), undefined, setSaveUserSuccess, false);
    }

    const [userSet, setUserSet] = useState(
        new userSettings(
            user.sub, "", 
            new Settings(undefined, false),
            new Image(undefined, "")
        ));

    const [userSetSuccess, setUserSetSuccess] = useState();
    useEffect(() => {
        setUserSetSuccess("waiting");
        arw.request({isAuthenticated, getAccessTokenSilently}, baseUrlUser, 'user/my', 'GET', undefined, setUserSet, setUserSetSuccess, false);
    }, []);
    
    
    return (
        <>
        <Container className="mb-5">
            <Row className="d-flex mb-3">
                <Col></Col>
                <Col>
                    <h1>Benutzer {userSet.userName}</h1>
                </Col>
                <Col className="text-end">
                    <LogOutButton />
                </Col>
            </Row>
            <Row className="justify-content-center mb-5">
                <ImageUploaderUser handleChangeFunction={handleChangeImage} passedImage={userSet.image.base64data} />
            </Row>
            <Row className="justify-content-center mb-3">
                <Col sm={9} md={7} lg={5}>
                    <FloatingLabel controlId="floatingUserName" label="Benutzername">
                        <Form.Control placeholder="Benutzername" onChange={(e) => handleChangeUsername(e)} value={userSet.userName}
                            maxLength={50}
                        />
                    </FloatingLabel>
                </Col>
            </Row>
            <Row className="justify-content-center mb-3">
                <Col sm={9} md={7} lg={5}>
                    <FloatingLabel controlId='floatingShowUserName'>
                        <Form.Check type="checkbox" label="Benutzername anzeigen" onChange={(e) => handleChangeShowUsername(e)} checked={userSet.settings.showUserNameInRecipe}/>
                    </FloatingLabel>
                </Col>
            </Row>
            <Row className="justify-content-center mb-3">
                <Col className='text-center'>
                    <Button variant="primary" className="me-1" onClick={() => handleSave()}>Speichern</Button>
                </Col>
            </Row>
            <Row className="justify-content-center mb-3">
                <Col className="text-center">
                {saveUserSuccess === "waiting" ?
                    <Spinner animation="border" role="status" />
                : null}
                {saveUserSuccess === "success" ?
                    <div>Speichern erfolgreich</div>
                : null}
                {saveUserSuccess === "error" ?
                    <div>Speichern fehlgeschlagen</div>
                : null}
                </Col>
            </Row>
        </Container>
        </>
    );
}
export default User;