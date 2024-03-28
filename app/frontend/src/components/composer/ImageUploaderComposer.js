import Button from 'react-bootstrap/Button';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Image from 'react-bootstrap/Image';
import Row from 'react-bootstrap/Row';

import imgplaceholder from '../../image-placeholder.jpeg';
import { useContext, useEffect, useState } from 'react';
import { Modal, Spinner } from 'react-bootstrap';
import { useAuth0 } from '@auth0/auth0-react';
import { AuthenticatedRequestWrapperContext } from '../../App';
import { baseUrlRecipes } from '../../config/config';
import ImageEditor from '../general/ImageEditor';

function ImageUploaderComposer({handleChangeFunction, recipe, validationFunction, isAuthenticated, getAccessTokenSilently, user, showModal, setShowModal}) {
    const arw = useContext(AuthenticatedRequestWrapperContext);
    //const [isAuthenticated, getAccessTokenSilently] = useAuth0();

    const [image, setImage] = useState();
    const [genImage, setGenImage] = useState();
    const [genSuccess, setGenSuccess] = useState('');
    const [editMode, setEditMode] = useState(false);

    const handleModalClose = () => setShowModal(false);
    const handleModalShow = () => setShowModal(true);

    const handleImageChangeUpload = async (e) => {
        let base64File = await convertToBase64(e.target.files[0]);
        setImage(base64File);
        setEditMode(true);
        handleChangeFunction(
            {
                prompt: '',
                base64data: base64File,
            });
    }

    const handleImageChangeCrop = (base64File) => {
        setImage(base64File);
        setEditMode(false);
        handleChangeFunction(
            {
                prompt: '',
                base64data: base64File,
            });
    }

    const convertToBase64 = (file) => {
        return new Promise((resolve, reject) => {
            const fileReader = new FileReader();
            fileReader.readAsDataURL(file);
            fileReader.onload = () => {
                resolve(fileReader.result);
            };
            fileReader.onerror = (error) => {
                reject(error);
            };
        });
    };

    const handleGenerateImage = () => {
        let tmp = {...recipe};
        tmp.image = {prompt: '', base64data:''};
        tmp.createdBy = user.sub;
        console.log('Generate Image for recipe:');
        console.log(tmp);
        setGenSuccess('waiting');
        arw.request({isAuthenticated, getAccessTokenSilently}, baseUrlRecipes, 'recipe/genimage', 'POST', JSON.stringify(tmp), setGenImage, setGenSuccess, true);
    }

    const handleImageChangeGen = (data) => {
        setImage(genImage.base64data);
        handleChangeFunction(genImage);
    }

    useEffect(() => {
        if (recipe.image && recipe.image.base64data) {
            setImage(recipe.image.base64data);
        }
    }, [recipe]);

    return(
        <>
        <Container className='text-center'>
            <Row>
                <Col>
                    <ImageEditor inputImageB64={image} getEditedImageB64={handleImageChangeCrop} edit={editMode} setEdit={setEditMode} />
                    {/*<Image src={image? image : imgplaceholder} style={{maxHeight: 250, maxWidth: 250}} className="mb-3" rounded />*/}
                </Col>
            </Row>
            <Row>
                <Col>
                <Button variant="primary" className="me-1" onClick={()=> document.getElementById('uploadInput').click()}>Hochladen</Button>
                <input id='uploadInput' hidden type="file"
                        accept='image/*, capture=camera'
                        onChange={(e) => handleImageChangeUpload(e)} />
                <Button variant="secondary" onClick={handleModalShow}>Generieren</Button>
                </Col>
            </Row>
        </Container>
        <Modal show={showModal} onHide={handleModalClose}>
            <Modal.Header closeButton>
                <Modal.Title>Rezepbild generieren</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <Row>
                    <p>
                        {!validationFunction(recipe)? 
                            'Bitte fülle zuerst alle Felder aus.' 
                        : 
                            'Drücke den Button um ein Bild für das Rezept zu generieren. Dies kann einige Sekunden dauern.'}
                    </p>
                </Row>
                <Row className="justify-content-center mb-3">
                        <Image src={genImage? genImage.base64data : imgplaceholder} style={{maxHeight: 250, maxWidth: 250}} className="mb-3" rounded />
                </Row>
                <Row>
                    <p>
                        {genSuccess === 'error' &&
                            'Beim Generieren des Bildes ist ein Fehler aufgetreten. Das tut uns leid. Bitte versuche es erneut.'
                        }
                    </p>
                </Row>
            </Modal.Body>
            <Modal.Footer>
            <Button variant="secondary" onClick={handleModalClose}>
                Abbrechen
            </Button>
            <Button variant="success" disabled={!validationFunction(recipe) || genSuccess == 'waiting'} onClick={handleGenerateImage}>
               {genSuccess === '' && 
                    'Generieren'
                }
                {(genSuccess === 'success' || genSuccess === 'error') &&
                    'Erneut generieren'
                }
                {genSuccess === 'waiting' &&
                <>
                    Generiere Bild&nbsp;
                    <Spinner
                        as="span"
                        animation="border"
                        size="sm"
                        role="status"
                        aria-hidden="true"
                        />
                </>
                }
            </Button>
            <Button variant="primary" disabled={!genImage} onClick={()=> {handleImageChangeGen(); handleModalClose();}}>
                Bild übernehmen
            </Button>
            </Modal.Footer>
        </Modal>
        </>
    );
}

export default ImageUploaderComposer;