#include "TunnelReduction.h"
#include "Z3Tools.h"
#include "stdio.h"

/**
 * @brief Creates the variable "x_{node,pos,stack_height}" of the reduction (described in the subject).
 *
 * @param ctx The solver context.
 * @param node A node.
 * @param pos The path position.
 * @param stack_height The highest cell occupied of the stack at that position.
 * @return Z3_ast
 */
Z3_ast tn_path_variable(Z3_context ctx, int node, int pos, int stack_height)
{
    char name[60];
    snprintf(name, 60, "node %d, pos %d, height %d", node, pos, stack_height);
    return mk_bool_var(ctx, name);
}

/**
 * @brief Creates the variable "y_{pos,height,4}" of the reduction (described in the subject).
 *
 * @param ctx The solver context.
 * @param pos The path position.
 * @param height The height of the cell described.
 * @return Z3_ast
 */
Z3_ast tn_4_variable(Z3_context ctx, int pos, int height)
{
    char name[60];
    snprintf(name, 60, "4 at height %d on pos %d", height, pos);
    return mk_bool_var(ctx, name);
}

/**
 * @brief Creates the variable "y_{pos,height,6}" of the reduction (described in the subject).
 *
 * @param ctx The solver context.
 * @param pos The path position.
 * @param height The height of the cell described.
 * @return Z3_ast
 */
Z3_ast tn_6_variable(Z3_context ctx, int pos, int height)
{
    char name[60];
    snprintf(name, 60, "6 at height %d on pos %d", height, pos);
    return mk_bool_var(ctx, name);
}

/**
 * @brief Wrapper to have the correct size of the array representing the stack (correct cells of the stack will be from 0 to (get_stack_size(length)-1)).
 *
 * @param length The length of the sought path.
 * @return int
 */
int get_stack_size(int length)
{
    return length / 2 + 1;
}

void tn_get_path_from_model(Z3_context ctx, Z3_model model, TunnelNetwork network, int bound, tn_step *path)
{
    int num_nodes = tn_get_num_nodes(network);
    int stack_size = get_stack_size(bound);
    for (int pos = 0; pos < bound; pos++)
    {
        int src = -1;
        int src_height = -1;
        int tgt = -1;
        int tgt_height = -1;
        for (int n = 0; n < num_nodes; n++)
        {
            for (int height = 0; height < stack_size; height++)
            {
                if (value_of_var_in_model(ctx, model, tn_path_variable(ctx, n, pos, height)))
                {
                    src = n;
                    src_height = height;
                }
                if (value_of_var_in_model(ctx, model, tn_path_variable(ctx, n, pos + 1, height)))
                {
                    tgt = n;
                    tgt_height = height;
                }
            }
        }
        int action = 0;
        if (src_height == tgt_height)
        {
            if (value_of_var_in_model(ctx, model, tn_4_variable(ctx, pos, src_height)))
                action = transmit_4;
            else
                action = transmit_6;
        }
        else if (src_height == tgt_height - 1)
        {
            if (value_of_var_in_model(ctx, model, tn_4_variable(ctx, pos, src_height)))
            {
                if (value_of_var_in_model(ctx, model, tn_4_variable(ctx, pos + 1, tgt_height)))
                    action = push_4_4;
                else
                    action = push_4_6;
            }
            else if (value_of_var_in_model(ctx, model, tn_4_variable(ctx, pos + 1, tgt_height)))
                action = push_6_4;
            else
                action = push_6_6;
        }
        else if (src_height == tgt_height + 1)
        {
            {
                if (value_of_var_in_model(ctx, model, tn_4_variable(ctx, pos, src_height)))
                {
                    if (value_of_var_in_model(ctx, model, tn_4_variable(ctx, pos + 1, tgt_height)))
                        action = pop_4_4;
                    else
                        action = pop_6_4;
                }
                else if (value_of_var_in_model(ctx, model, tn_4_variable(ctx, pos + 1, tgt_height)))
                    action = pop_4_6;
                else
                    action = pop_6_6;
            }
        }
        path[pos] = tn_step_create(action, src, tgt);
    }
}

void tn_print_model(Z3_context ctx, Z3_model model, TunnelNetwork network, int bound)
{
    int num_nodes = tn_get_num_nodes(network);
    int stack_size = get_stack_size(bound);
    for (int pos = 0; pos < bound + 1; pos++)
    {
        printf("At pos %d:\nState: ", pos);
        int num_seen = 0;
        for (int node = 0; node < num_nodes; node++)
        {
            for (int height = 0; height < stack_size; height++)
            {
                if (value_of_var_in_model(ctx, model, tn_path_variable(ctx, node, pos, height)))
                {
                    printf("(%s,%d) ", tn_get_node_name(network, node), height);
                    num_seen++;
                }
            }
        }
        if (num_seen == 0)
            printf("No node at that position !\n");
        else
            printf("\n");
        if (num_seen > 1)
            printf("Several pair node,height!\n");
        printf("Stack: ");
        bool misdefined = false;
        bool above_top = false;
        for (int height = 0; height < stack_size; height++)
        {
            if (value_of_var_in_model(ctx, model, tn_4_variable(ctx, pos, height)))
            {
                if (value_of_var_in_model(ctx, model, tn_6_variable(ctx, pos, height)))
                {
                    printf("|X");
                    misdefined = true;
                }
                else
                {
                    printf("|4");
                    if (above_top)
                        misdefined = true;
                }
            }
            else if (value_of_var_in_model(ctx, model, tn_6_variable(ctx, pos, height)))
            {
                printf("|6");
                if (above_top)
                    misdefined = true;
            }
            else
            {
                printf("| ");
                above_top = true;
            }
        }
        printf("\n");
        if (misdefined)
            printf("Warning: ill-defined stack\n");
    }
    return;
}

/**
 * @brief Check if the node can make an operation to another node.
 *
 * @param ctx The solver context.
 * @param network The network we are working in.
 * @param node The current node.
 * @param pos The path position.
 * @param height The height of the cell described.
 * @param bound The maximum size of the pile.
 * @return Z3_ast
 */
Z3_ast valid_path(Z3_context ctx, const TunnelNetwork network, int node, int pos, int h, int bound){
    int nb = tn_get_num_nodes(network);
    Z3_ast res[nb];
    for(int n = 0; n<nb; n++){
        if(tn_is_edge(network, node, n)){
            Z3_ast temp[NumActions];
            for(int a = 0; a<NumActions; a++){
                if(tn_node_has_action(network, node, a)){
                    switch (a) {
                        case transmit_4:
                        case transmit_6: temp[a] = transmitting(ctx, a==transmit_4?4:6, pos, h, n, node); break;
                        case push_4_4: temp[a] = pushing(ctx, 4, 4, pos, h, n, bound, node); break;
                        case push_4_6: temp[a] = pushing(ctx, 4, 6, pos, h, n, bound, node); break;
                        case push_6_4: temp[a] = pushing(ctx, 6, 4, pos, h, n, bound, node); break;
                        case push_6_6: temp[a] = pushing(ctx, 6, 6, pos, h, n, bound, node); break;
                        case pop_4_4: case pop_4_6: case pop_6_4:
                        case pop_6_6: temp[a] = popping(ctx, (a<=7)?4:6, (a%2==0)?4:6, pos, h, n, node); break;
                        default: temp[a] = Z3_mk_false(ctx);
                    } 
                }
                else temp[a] = Z3_mk_false(ctx); 
                
            }
            res[n] = Z3_mk_or(ctx, NumActions, temp);
        }
        else res[n] =Z3_mk_false(ctx); 
    }
    return Z3_mk_or(ctx, nb, res);
}


Z3_ast tn_reduction(Z3_context ctx, const TunnelNetwork network, int length)
{

    int size_formula = 6, hmax = get_stack_size(length);
    Z3_ast final[size_formula];
    final[0] = pile_p1(ctx, network, length, hmax);
    final[1] = simple_path(ctx, network, hmax, length);
    final[2] = start_end_condition(ctx, network, hmax, length);
    final[3] = pile_p2(ctx, network, length, hmax);
    int nb_nodes = tn_get_num_nodes(network);
  
    Z3_ast result[length];
    Z3_ast res[nb_nodes * hmax];  
    for (int p = 0; p < length; p++) {
        int cpt = 0;
        for (int n = 0; n < nb_nodes; n++) {
            for (int h = 0; h < hmax; h++) {
                Z3_ast current_state = tn_path_variable(ctx, n, p, h);
                Z3_ast possible_transitions = valid_path(ctx, network, n, p, h, hmax);
                res[cpt++] = Z3_mk_implies(ctx, current_state, possible_transitions);
            }
        }
        result[p] = Z3_mk_and(ctx, nb_nodes * hmax, res);
    }
    final[4] = Z3_mk_and(ctx, length, result);
    final[5] = My_At_Most(ctx, network, hmax, length);

    return Z3_mk_and(ctx, size_formula, final);
}


/**
 * @brief Check every position in the path to be sure that if there is a node, the pile can't have a 4 and a 6 at the same time
 * @param ctx The solver context.
 * @param network A Tunnel Network.
 * @param l The size of the target path.
 * @param hmax The max size of the stack.
 * @return Z3_ast
 * @pre @p network must be initialized.
 */

Z3_ast pile_p1(Z3_context ctx, TunnelNetwork network, int l, int hmax){
    Z3_ast result_l[l+1];

    for (int p = 0; p < l+1; p++) {
        Z3_ast result_h[hmax-1];
        for (int h = 1; h < hmax; h++) {

            Z3_ast exist_n[tn_get_num_nodes(network)];
            for (int n = 0; n < tn_get_num_nodes(network); n++) {
                exist_n[n] = tn_path_variable(ctx, n, p, h);
            }
            Z3_ast exists = Z3_mk_or(ctx, tn_get_num_nodes(network), exist_n);

            Z3_ast left_y[2]  = { tn_4_variable(ctx,p,h), Z3_mk_not(ctx, tn_6_variable(ctx,p,h)) };
            Z3_ast right_y[2] = { Z3_mk_not(ctx, tn_4_variable(ctx,p,h)), tn_6_variable(ctx,p,h) };

            Z3_ast xor[2] = {
                Z3_mk_and(ctx, 2, left_y),
                Z3_mk_and(ctx, 2, right_y)
            };

            result_h[h-1] = Z3_mk_implies(ctx, exists, Z3_mk_or(ctx, 2, xor));
        }
        result_l[p] = Z3_mk_and(ctx, hmax - 1, result_h);
    }

    return Z3_mk_and(ctx, l+1, result_l);
}

/**
 * @brief Check for each height under the current node that there is another and the stack isn't weird
 * @param ctx The solver context.
 * @param network A Tunnel Network.
 * @param h The height we are looking from.
 * @param n The node we currently are.
 * @param p The position we currently are.
 * @return Z3_ast
 * @pre @p network must be initialized.
 */

Z3_ast B(Z3_context ctx, TunnelNetwork network, int h, int n, int p){
    Z3_ast res[h];
    for(int h_prime = 0; h_prime<h; h_prime++){
        Z3_ast not_x = Z3_mk_not(ctx, tn_path_variable(ctx, n, p, h));
        Z3_ast y_4 = tn_4_variable(ctx, p, h_prime);
        Z3_ast y_6 = tn_6_variable(ctx, p, h_prime);
        Z3_ast res_or[3];
        res_or[0] = not_x;
        res_or[1] = y_4;
        res_or[2] = y_6;

        res[h_prime] = Z3_mk_or(ctx, 3, res_or);
    }

    return Z3_mk_and(ctx, h, res);
}

/**
 * @brief Check for each height over the current node that there is nothing
 * @param ctx The solver context.
 * @param network A Tunnel Network.
 * @param h The height we are looking from.
 * @param n The node we currently are.
 * @param p The position we currently are.
 * @param hmax The maximum height of the stack.
 * @return Z3_ast
 * @pre @p network must be initialized.
 */
Z3_ast A(Z3_context ctx, TunnelNetwork network, int h, int hmax, int n, int p){
    if(hmax-(h+1) == 0) return Z3_mk_true(ctx);
    Z3_ast real_res[hmax-(h+1)];
    int cpt = 0;
    for(int h_prime = h+1; h_prime<hmax; h_prime++){

        Z3_ast not_x = Z3_mk_not(ctx, tn_path_variable(ctx, n, p, h));
        Z3_ast not_y_4 = Z3_mk_not(ctx, tn_4_variable(ctx, p, h_prime));
        Z3_ast not_y_6 = Z3_mk_not(ctx, tn_6_variable(ctx, p, h_prime));

        Z3_ast re[2];
        re[0] = not_x;
        re[1] = not_y_4;

        Z3_ast r[2];
        r[0] = not_x;
        r[1] = not_y_6;

        Z3_ast res[2];
        res[0] = Z3_mk_or(ctx, 2, r);
        res[1] = Z3_mk_or(ctx, 2, re);

        real_res[cpt++] = Z3_mk_and(ctx, 2, res);
    }

    return Z3_mk_and(ctx, hmax-(h+1), real_res);
}


/**
 * @brief Check the stack has the right value on his top after an operation
 * @param ctx The solver context.
 * @param network A Tunnel Network.
 * @param l The size of the target path.
 * @param hmax The max size of the stack.
 * @return Z3_ast
 * @pre @p network must be initialized.
 */

Z3_ast pile_p2(Z3_context ctx, TunnelNetwork network, int l, int hmax){
    int nb_node = tn_get_num_nodes(network);
    Z3_ast res[nb_node];
    for(int n = 0; n<nb_node; n++){
        Z3_ast re[l+1];
        for(int p = 0; p<l+1; p++){
            Z3_ast r[hmax];
            for(int h = 0; h<hmax; h++){
                Z3_ast x = tn_path_variable(ctx, n, p, h);
                Z3_ast right_part[2];
                right_part[0] = A(ctx, network, h, hmax, n, p);
                right_part[1] = B(ctx, network, h, n, p);

                r[h] = Z3_mk_implies(ctx, x, Z3_mk_and(ctx, 2, right_part));
            }
            re[p] = Z3_mk_and(ctx, hmax, r);
        }
        res[n] = Z3_mk_and(ctx, l+1, re);
    }
    return Z3_mk_and(ctx, nb_node, res);
}

/**
 * @brief Check the start and the end of the path.
 * @param ctx The solver context.
 * @param network A Tunnel Network.
 * @param l The size of the target path.
 * @param hmax The max size of the stack.
 * @return Z3_ast 
 * @pre @p network must be initialized.
 */

Z3_ast start_end_condition(Z3_context ctx, TunnelNetwork network, int hmax, int l) {
    int nb_node = tn_get_num_nodes(network);
    int s = tn_get_initial(network);

    Z3_ast start_true = tn_path_variable(ctx, s, 0, 0);
    Z3_ast start_stack = tn_4_variable(ctx, 0, 0);
    Z3_ast not_6 = Z3_mk_not(ctx, tn_6_variable(ctx, 0, 0));

    int end = tn_get_final(network);
    Z3_ast end_true = tn_path_variable(ctx, end, l, 0);
    Z3_ast end_stack = tn_4_variable(ctx, l, 0);

    Z3_ast others_false_start[nb_node-1];
    Z3_ast others_false_end[nb_node-1];
    int cpt_start = 0;
    int cpt_end = 0;
    for(int n = 0; n < nb_node; n++) {
        if(n == s){
            others_false_end[cpt_end++] = Z3_mk_not(ctx, tn_path_variable(ctx, n, l, 0));
            continue;
        } 
        if(n == end){
            others_false_start[cpt_start++] = Z3_mk_not(ctx, tn_path_variable(ctx, n, 0, 0));
            continue;
        } 
        others_false_start[cpt_start++] = Z3_mk_not(ctx, tn_path_variable(ctx, n, 0, 0));
        others_false_end[cpt_end++] = Z3_mk_not(ctx, tn_path_variable(ctx, n, l, 0));
    }
    Z3_ast no_other_start = Z3_mk_and(ctx, nb_node-1, others_false_start);
    Z3_ast no_other_end = Z3_mk_and(ctx, nb_node-1, others_false_end);
    
    Z3_ast forbid_h_start[hmax-1], forbid_h_end[hmax-1];
    for(int h = 1; h < hmax; h++) {
        Z3_ast forbid_nodes_start[nb_node], forbid_nodes_end[nb_node];
        for(int n = 0; n < nb_node; n++) {
            forbid_nodes_start[n] = Z3_mk_not(ctx, tn_path_variable(ctx, n, 0, h));
            forbid_nodes_end[n] = Z3_mk_not(ctx, tn_path_variable(ctx, n, l, h));
        }
        forbid_h_start[h-1] = Z3_mk_and(ctx, nb_node, forbid_nodes_start);
        forbid_h_end[h-1] = Z3_mk_and(ctx, nb_node, forbid_nodes_end);
    }
    Z3_ast no_other_height_start = Z3_mk_and(ctx, hmax-1, forbid_h_start);
    Z3_ast no_other_height_end = Z3_mk_and(ctx, hmax-1, forbid_h_end);

    Z3_ast all[9] = { start_true, no_other_start, no_other_height_start, start_stack, not_6, end_true, end_stack, no_other_height_end, no_other_end};
    return Z3_mk_and(ctx, 9, all);
}


/**
 * @brief Check if there is a simple path of size l in the network
 * @param ctx The solver context.
 * @param network A Tunnel Network.
 * @param l The size of the target path.
 * @param hmax The max size of the stack.
 * @return Z3_ast
 * @pre @p network must be initialized.
 */
Z3_ast simple_path(Z3_context ctx, TunnelNetwork network, int hmax, int l) {
    int nb_node = tn_get_num_nodes(network);

    Z3_ast and_l[l+1];

    for(int p =0; p<l+1; p++){
        int cpt = 0;
        Z3_ast n_h[nb_node*hmax];
        for(int h = 0; h<hmax; h++){
            for(int n = 0; n<nb_node; n++)
                n_h[cpt++] = tn_path_variable(ctx, n, p, h);
        }
        and_l[p] = uniqueFormula(ctx, n_h, nb_node*hmax);
    }
    return Z3_mk_and(ctx, l+1, and_l);
}

/**
 * @brief Our implementation of At_most
 * @param ctx The solver context.
 * @param network A Tunnel Network.
 * @param l The size of the target path.
 * @param hmax The max size of the stack.
 * @return Z3_ast The formula
 * @pre @p network must be initialized.
 */

Z3_ast My_At_Most(Z3_context ctx, TunnelNetwork network, int hmax, int l){
    int nb_node = tn_get_num_nodes(network);

    Z3_ast for_n[nb_node];

    for(int n = 0; n<nb_node; n++){
        int cpt = 0;
        Z3_ast for_ph[(l+1) * hmax];
        for(int p = 0; p<l+1; p++){
            for(int h = 0; h<hmax; h++)
                for_ph[cpt++] = tn_path_variable(ctx, n, p, h); 
        }
        for_n[n] = at_most_formula(ctx, for_ph, (l+1) * hmax);
    }

    return Z3_mk_and(ctx, nb_node, for_n);
}


/**
 * @brief Each stage of the stack as the same value at a position and the next one (not last h if pop)
 * @param ctx The solver context.
 * @param network A Tunnel Network.
 * @param pos The position we currently are.
 * @param h The height of the stack.
 * @return Z3_ast
 * @pre @p network must be initialized.
 */

Z3_ast equivalent(Z3_context ctx, int pos, int h) {
    if(h<0)return Z3_mk_false(ctx);
    Z3_ast res[2], result[h+1];
    for (int i = 0; i <= h; i++) {
        res[0] = Z3_mk_eq(ctx, tn_4_variable(ctx, pos+1, i), tn_4_variable(ctx, pos, i));
        res[1] = Z3_mk_eq(ctx, tn_6_variable(ctx, pos+1, i), tn_6_variable(ctx, pos, i));
        result[i] = Z3_mk_and(ctx, 2, res);
    }  
    return Z3_mk_and(ctx, h+1, result);
}


/**
 * @brief Verifying a node transmit correctly
 * @param ctx The solver context.
 * @param a The transmitting value.
 * @param pos The position we currently are.
 * @param h The current height of the stack.
 * @param current The node doing the transmission.
 * @param next A different node comparing to current.
 * @return Z3_ast The formula
 * @pre @p network must be initialized.
 */
Z3_ast transmitting(Z3_context ctx, int a, int pos, int h, int next, int current) {
    Z3_ast x = tn_path_variable(ctx, current, pos, h);
    Z3_ast res[4];
    res[0] = (a==4) ? tn_4_variable(ctx, pos, h):tn_6_variable(ctx, pos, h);
    res[1] = tn_path_variable(ctx, next, pos+1, h);
    res[2] = equivalent(ctx, pos, h);
    res[3] = (a==4) ? tn_4_variable(ctx, pos+1, h):tn_6_variable(ctx, pos+1, h);
    return Z3_mk_implies(ctx, x, Z3_mk_and(ctx, 4, res)) ;
}


/**
 * @brief Verifying a node push correctly
 * @param ctx The solver context.
 * @param a The current value of the top of the stack.
 * @param b The pushing value.
 * @param pos The position we currently are.
 * @param h The current height of the stack.
 * @param current The node doing the pushing.
 * @param next A different node comparing to current.
 * @param bound The maximum size of the stack.
 * @return Z3_ast
 * @pre @p network must be initialized.
 */
Z3_ast pushing(Z3_context ctx, int a, int b, int pos, int h, int next, int bound, int current) {
    if (h >= bound) return Z3_mk_false(ctx);
    Z3_ast x = tn_path_variable(ctx, current, pos, h);
    Z3_ast result[4];
    result[0] = (a==4) ? tn_4_variable(ctx, pos, h):tn_6_variable(ctx, pos, h);
    result[1] = tn_path_variable(ctx, next, pos+1, h+1);
    result[2] = (b==4) ? tn_4_variable(ctx, pos+1, h+1):tn_6_variable(ctx, pos+1, h+1);
    result[3] = equivalent(ctx, pos, h);
    return Z3_mk_implies(ctx, x, Z3_mk_and(ctx, 4, result)) ;
}


/** 
 * @brief Verifying a node pop correctly
 * @param ctx The solver context.
 * @param a The current value of the top of the stack.
 * @param b The popping value.
 * @param pos The position we currently are.
 * @param h The current height of the stack.
 * @param current The node doing the popping.
 * @param next A different node comparing to current.
 * @return Z3_ast
 * @pre @p network must be initialized.
 */
Z3_ast popping(Z3_context ctx, int a, int b, int pos, int h, int next, int current) {
    if (h < 1) return Z3_mk_false(ctx);
    Z3_ast x = tn_path_variable(ctx, current, pos, h);
    Z3_ast result[4];
    result[0] = (b==4) ? tn_4_variable(ctx, pos, h):tn_6_variable(ctx, pos, h);
    result[1] = tn_path_variable(ctx, next, pos+1, h-1);
    result[2] = equivalent(ctx, pos, h-1);
    result[3] = (a==4) ? tn_4_variable(ctx, pos+1, h-1):tn_6_variable(ctx, pos+1, h-1);
    
    return Z3_mk_implies(ctx, x,Z3_mk_and(ctx, 4, result));
}