import Button from 'react-bootstrap/Button';
import Col from 'react-bootstrap/Col';
import Container from 'react-bootstrap/Container';
import Image from 'react-bootstrap/Image';
import Row from 'react-bootstrap/Row';

import imgplaceholder from '../../../image-placeholder.jpeg';
import { useEffect, useState } from 'react';
import ImageEditor from '../../general/ImageEditor';

function ImageUploaderUser({handleChangeFunction, passedImage}) {

    const [image, setImage] = useState();
    const [editMode, setEditMode] = useState(false);
    const handleImageChange = async (e) => {
        let base64File = await convertToBase64(e.target.files[0]);
        setImage(base64File);
        setEditMode(true);
        handleChangeFunction(base64File);
    }

    const handleImageCrop = (base64File) => {
        setImage(base64File);
        setEditMode(false);
        handleChangeFunction(base64File);
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

    useEffect(() => {
        setImage(passedImage);
    } , [passedImage]);

    return(
        <Container className='text-center'>
            <Row>
                <Col>
                {/*<Image src={image? image : imgplaceholder} style={{height: 250, width: 250}} className="mb-3" rounded />*/}
                <ImageEditor inputImageB64={image} getEditedImageB64={handleImageCrop} edit={editMode} setEdit={setEditMode} />
                </Col>
            </Row>
            <Row>
                <Col>
                    <Button type="file" variant="primary" className="me-1" onClick={()=> document.getElementById('uploadInput').click()}>Bild ändern</Button>
                    <input id='uploadInput' hidden type="file"
                        accept='image/*, capture=camera'
                        onChange={(e) => handleImageChange(e)} />
                </Col>
            </Row>
        </Container>
    );
}

export default ImageUploaderUser;