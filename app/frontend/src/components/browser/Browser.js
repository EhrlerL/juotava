import { useContext, useEffect, useState } from 'react';
import { useNavigate } from 'react-router';
import { useAuth0 } from '@auth0/auth0-react';
import { AuthenticatedRequestWrapperContext } from '../../App';
import {baseUrlRecipes } from '../../config/config';
import {Button, Col, Container, Row, Spinner} from 'react-bootstrap'
import RecipeCard from '../general/RecipeCard';
import Filter from './filter/Filter';

function Browser(props) {
    const navigate = useNavigate();
    const arw = useContext(AuthenticatedRequestWrapperContext);
    const {user, isAuthenticated, getAccessTokenSilently} = useAuth0();

    const [recipeExcerpts, setRecipeExcerpts] = useState([]);
    const [loadRecipeExcerptsSuccess, setLoadRecipeExcerptsSuccess] = useState('');

    // send Request: RecipeExcerpts
    const [refresh, setRefresh] = useState(0);
    // triggers in Filter when filter is saved
    const triggerRefresh = () => {
        setRefresh(refresh + 1);
    }
    useEffect(() => {
        setLoadRecipeExcerptsSuccess('waiting');
        arw.request({isAuthenticated, getAccessTokenSilently}, baseUrlRecipes, 'recipeexcerpt/all', 'GET', undefined, setRecipeExcerpts, setLoadRecipeExcerptsSuccess, true);
    }, [refresh]);

    const handleOpenRecipe = ((uuid) => {
        navigate('/browser/recipe/'+uuid);
    });

    return (
        <Container fluid className='mb-5'>
            <Row>
                <Col sm='3'>
                    <Filter loadRecipeExcerptsSuccess={loadRecipeExcerptsSuccess} triggerRefresh={triggerRefresh} />
                </Col>
                <Col sm='8'>
                    {loadRecipeExcerptsSuccess === 'waiting' ?
                        <h4 className="text-center my-5">
                            Lade Rezepte <br />
                            <Spinner animation="border" role="status" />
                        </h4>
                    : ''
                    }
                    {loadRecipeExcerptsSuccess === 'error' ?
                        <h4 className="text-center my-5">
                            Beim Laden der Rezepte ist ein Fehler aufgetreten, das tut uns leid!
                        </h4>
                    : ''
                    }

                        {loadRecipeExcerptsSuccess === 'success' && recipeExcerpts.length === 0 ? 
                            <h4 className="text-center my-5">
                                Wir konnten leider keine passenden Rezepte finden...
                            </h4>
                        : (
                            recipeExcerpts.map((item, index) => {
                                return(
                                    <Row key={index} className="mb-3" style={{margin: 0}}>
                                        {item.title}
                                        <RecipeCard 
                                            id={item.uuid} 
                                            handleClick={() => handleOpenRecipe(item.uuid)} 
                                            excerpt={item}
                                        />
                                    </Row>
                                );
                            })
                        )}
                </Col>
            </Row>
        </Container>
    );
}
export default Browser;